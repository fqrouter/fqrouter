/*
 * Netfilter module implementation of west-chamber-season-2. 
 * 
 * Designed for netizens in P.R.C. to access censored information freely. 
 *
 * Copyright (c) 2010 Mike Chen
 * Author: Mike Chen 
 * Contact: i@ccp.li
 *
 * Great thanks to Jan Engelhardt, Nicolas Bouliane for this documentation: 
 * http://jengelh.medozas.de/documents/Netfilter_Modules.pdf
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 */

#include <linux/netfilter.h>
#include <linux/ip.h>
#include <linux/ipv6.h>
#include <linux/netfilter/x_tables.h>
#include <linux/skbuff.h>

#include <net/tcp.h>
#include <net/udp.h>
#include <linux/byteorder/generic.h>

#include "xt_wcs2.h"

static unsigned int
xt_wcs2_tg(struct sk_buff *skb, const struct xt_target_param *par)
{
	const struct xt_wcs2_tg_info *info;
	__u16 payload_len = 0;
	int extra_space_needed;
	int i;
	struct inet_sock *inet;
	struct flowi *fl;
	struct udphdr *udph;
	struct iphdr *iph;
	__be32 saddr;

	info = par->targinfo;

	/* Save the packet length, it will be the payload length after packing. */
	payload_len = skb->len;
	iph = (struct iphdr *)skb_network_header(skb);

	/* Save the source addr. */
	saddr = iph->saddr;

	/* Encrypt the packet if specified. */
	if (info->xor_key != 0) {	
		for (i = 0; i < payload_len; i++) {
			(*((char*)iph + i)) ^= info->xor_key;
		}
	}

	/* Do checksum of payload. */
	skb->csum = csum_partial(iph, payload_len, 0);

	if (!skb_make_writable(skb, skb->len)) {
		pr_info("iptables: xt_wcs2: skb_make_writable faied. ");
		return NF_DROP;
	}

	/* Make sure there is enough space to put UDP header 
	   and allocate some memory if not enough. */
	extra_space_needed = sizeof(struct udphdr) + sizeof(struct iphdr);
	if (skb_headroom(skb) < extra_space_needed) {
		if (pskb_expand_head(skb, extra_space_needed, 0, GFP_ATOMIC)) {
			pr_info("iptables: xt_wcs2: no enough space and unable to allocate more. ");
			return NF_DROP;
		}
	}

	inet = inet_sk(skb->sk);
	fl = &inet->cork.fl;
	
	/* Get UDP header pointer. */
	udph = (struct udphdr*)skb_push(skb, sizeof(struct udphdr));

	/* Construct UDP header. */
#warning "FIXME: using fixed source port here. "
	udph->source = htons(58371);
	udph->dest = htons(info->udp_port);
	udph->len = htons(sizeof(struct udphdr) + payload_len);
	/* Do checksums of UDP header and combine the payload checksum */
	udph->check = 0;
	skb->csum = csum_partial((char *)udph, sizeof(struct udphdr), skb->csum);
	udph->check = csum_tcpudp_magic(saddr, info->gateway.in.s_addr, payload_len + sizeof(struct udphdr), IPPROTO_UDP, skb->csum);
	if (udph->check == 0)
		udph->check = -1;

	/* Get IP header. */
	iph = (struct iphdr*)skb_push(skb, sizeof(struct iphdr));

	/* Construct IP header. */
	iph->version = 4;
	iph->ihl = 5;
	iph->tos = inet->tos;
	iph->tot_len = htons(skb->len);
	iph->frag_off = htons(IP_DF);
	iph->id = htons(inet->id++);
	iph->ttl = 64;
	iph->protocol = IPPROTO_UDP;
	iph->saddr = saddr;
	iph->daddr = info->gateway.in.s_addr;
	ip_send_check(iph);

	/* Oh yeah, it's finished. */
	
	return NF_ACCEPT;
}

static bool xt_wcs2_tg_check(const struct xt_tgchk_param *par)
{
	pr_info("iptables: xt_wcs2: xt_wcs2_tg_check okay. ");
	return true;
}

static struct xt_target xt_wcs2_tg_reg __read_mostly = {
	.name		= "wcs2",
	.family		= AF_INET,
	.revision	= 0, 
	.table		= "mangle",
	.target		= xt_wcs2_tg,
	.targetsize	= sizeof(struct xt_wcs2_tg_info),
	.checkentry	= xt_wcs2_tg_check,
	.me		= THIS_MODULE,
};

static int __init xt_wcs2_init(void)
{
	return xt_register_target(&xt_wcs2_tg_reg);
}

static void __exit xt_wcs2_exit(void)
{
	xt_unregister_target(&xt_wcs2_tg_reg);
}

module_init(xt_wcs2_init);
module_exit(xt_wcs2_exit);
MODULE_LICENSE("GPL");
MODULE_AUTHOR("Mike Chen");
MODULE_DESCRIPTION("Netfilter module implementation of west-chamber-season-2. ");
MODULE_ALIAS("ipt_wcs2");

