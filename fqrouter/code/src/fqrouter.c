#include <stdio.h>
#include <stdlib.h>
#include <libnetfilter_queue/libnetfilter_queue.h>
#include <arpa/inet.h>
#include <linux/netfilter.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>


/*
 * SumWords()
 *
 * Do the one's complement sum thing over a range of words
 * Ideally, this should get replaced by an assembly version.
 */

static u_int32_t
/* static inline u_int32_t */
sum_words(u_int16_t *buf, int nwords)
{
  register u_int32_t	sum = 0;

  while (nwords >= 16)
  {
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    sum += (u_int16_t) ntohs(*buf++);
    nwords -= 16;
  }
  while (nwords--)
    sum += (u_int16_t) ntohs(*buf++);
  return(sum);
}

/*
 * IpChecksum()
 *
 * Recompute an IP header checksum
 */

void
recompute_ip_checksum(struct ip *ip)
{
  register u_int32_t	sum;

/* Sum up IP header words */

  ip->ip_sum = 0;
  sum = sum_words((u_int16_t *) ip, ip->ip_hl << 1);

/* Flip it & stick it */

  sum = (sum >> 16) + (sum & 0xFFFF);
  sum += (sum >> 16);
  sum = ~sum;

  ip->ip_sum = htons(sum);
}

/*
 * UdpChecksum()
 *
 * Recompute a UDP checksum on a packet
 *
 * UDP pseudo-header (prot = IPPROTO_UDP = 17):
 *
 *  | source IP address	       |
 *  | dest.  IP address	       |
 *  | zero | prot | UDP leng   |
 *
 * You must set udp->check = 0 for this to work correctly!
 * Or, if there's already a checksum there, then this will
 * set it to zero iff the checksum is correct.
 */

void
recompute_udp_checksum(struct ip *ip, struct udphdr *udp)
{
  u_int32_t	sum;

/* Do pseudo-header first */

  sum = sum_words((u_int16_t *) &ip->ip_src, 4);
  sum += (u_int16_t) IPPROTO_UDP;
  sum += (u_int16_t) ntohs(udp->len);

/* Now do UDP packet itself */

  sum += sum_words((u_int16_t *) udp,
	  ((u_int16_t) ntohs(udp->len)) >> 1);
  if (ntohs(udp->len) & 1)
    sum += (u_int16_t) (((u_char *) udp)[ntohs(udp->len) - 1] << 8);

/* Flip it & stick it */

  sum = (sum >> 16) + (sum & 0xFFFF);
  sum += (sum >> 16);
  sum = ~sum;

  udp->check = htons(sum);
}


static int pack(struct nfq_q_handle *qh,struct nfgenmsg *nfmsg, struct nfq_data *nfa, void *data2) {    
	int id = -1;
	struct nfqnl_msg_packet_hdr *packetHeader = NULL;
	int size_of_original_packet = -1;
	char *original_packet = NULL;
	struct ip *original_packet_ip = NULL;
	int size_of_new_packet = -1;
	unsigned char *new_packet = NULL; 
	struct ip *new_packet_ip = NULL;
	struct udphdr *new_packet_udphdr = NULL;
	unsigned char *new_packet_payload = NULL;
	if( (packetHeader = nfq_get_msg_packet_hdr(nfa)) != NULL ) {
		id = ntohl(packetHeader->packet_id);
	}
	size_of_original_packet = nfq_get_payload(nfa, &original_packet);
	printf("%d\n", size_of_original_packet);
	original_packet_ip = (struct ip*) original_packet;
	printf("from: %s\n", inet_ntoa(original_packet_ip->ip_src)); 
	printf("to: %s\n", inet_ntoa(original_packet_ip->ip_dst)); 
	size_of_new_packet = sizeof(struct ip) + sizeof(struct udphdr) + size_of_original_packet;
	new_packet = malloc(size_of_new_packet);
	new_packet_ip = (struct ip*) new_packet;
	memcpy(new_packet_ip, original_packet_ip, sizeof(struct ip));
	new_packet_ip->ip_hl = 5; // Fixed header
	new_packet_ip->ip_len = htons(size_of_new_packet);
	new_packet_ip->ip_p = IPPROTO_UDP; // UDP
	inet_aton("58.64.179.126", &new_packet_ip->ip_dst);
	recompute_ip_checksum(new_packet_ip);
	new_packet_udphdr = (struct udphdr*)(new_packet + sizeof(struct ip));
	new_packet_udphdr->source = 0;
	new_packet_udphdr->dest = htons(19840);
	new_packet_udphdr->len = htons(sizeof(struct udphdr) + size_of_original_packet);
	new_packet_payload  = new_packet + sizeof(struct ip) + sizeof(struct udphdr);
	memcpy(new_packet_payload, original_packet, size_of_original_packet);
	recompute_udp_checksum(new_packet_ip, new_packet_udphdr);
	nfq_set_verdict(qh, id, NF_ACCEPT, size_of_new_packet, new_packet);
	return id;
}

int main(int argc, char *argv[]) {
	struct nfq_handle *handle = NULL;
	struct nfq_q_handle *queue = NULL;
	struct nfnl_handle *netlink_handle = NULL;
	int nfqueue_fd;

	// NF_QUEUE initializing
	handle = nfq_open();
	if (!handle) {
		perror("Error: during nfq_open()");
		goto end;
	}
	if (nfq_unbind_pf(handle, AF_INET) < 0) {
		perror("Error: during nfq_unbind_pf()");
		goto end;
	}
	if (nfq_bind_pf(handle, AF_INET) < 0) {
		perror("Error: during nfq_bind_pf()");
		goto end;
	}
	queue = nfq_create_queue(handle, 0, &pack, NULL);
	if (!queue) {
		perror("Error: during nfq_create_queue()");
		goto end;
	}
	if (nfq_set_mode(queue, NFQNL_COPY_PACKET, 0xffff) < 0) {
		perror("Error: can't set packet_copy mode");
		goto end;
	}
	netlink_handle = nfq_nfnlh(handle);
	nfqueue_fd = nfnl_fd(netlink_handle);
	// End of NF_QUEUE initializing
	while(1) {
		char buf[4096] __attribute__ ((aligned));
		int received;
		received = recv(nfqueue_fd, buf, sizeof(buf), 0);
		if(received==-1) {
			break;
		}
		// Call the handle
		nfq_handle_packet(handle, buf, received);
	}
end:
	// Dispose all resources
	if(queue != NULL) {
		nfq_destroy_queue(queue);
	}
	if(handle != NULL) {
		nfq_close(handle);
	}
	return 0;
}