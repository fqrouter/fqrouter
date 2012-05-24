/* iptables module for proxy table
 *
 * (C) 2005 by Harald Welte <laforge@netfilter.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 */

#include <linux/module.h>
#include <linux/skbuff.h>
#include <linux/ip.h>
#include <linux/netfilter.h>
#include <linux/netfilter/x_tables.h>

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Qin Fen");
MODULE_DESCRIPTION("iptables module for proxy table");
MODULE_ALIAS("ipt_fqrouter");

static int __init xt_fqrouter_init(void)
{
	return 0;
}

static void __exit xt_fqrouter_exit(void)
{
}

module_init(xt_fqrouter_init);
module_exit(xt_fqrouter_exit);
