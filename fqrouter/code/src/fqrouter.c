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

static int handle_output(struct nfq_q_handle *qh,struct nfgenmsg *nfmsg, struct nfq_data *nfa, void *data2) {
	int id = -1;
	struct nfqnl_msg_packet_hdr *packetHeader = NULL;
	if( (packetHeader = nfq_get_msg_packet_hdr(nfa)) != NULL ) {
		id = ntohl(packetHeader->packet_id);
	}
	nfq_set_verdict(qh, id, NF_ACCEPT, 0, NULL);
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
	queue = nfq_create_queue(handle, 0, &handle_output, NULL);
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
	while(1) {
		char buf[4096] __attribute__ ((aligned));
		int received;
		received = recv(nfqueue_fd, buf, sizeof(buf), 0);
		if(received == -1) {
			break;
		}
		nfq_handle_packet(handle, buf, received);
	}
end:
	if(queue != NULL) {
		nfq_destroy_queue(queue);
	}
	if(handle != NULL) {
		nfq_close(handle);
	}
	return 0;
}
