/* iptables module for outbound/inbound table
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
#include <linux/netfilter_ipv4/ip_tables.h>
#include <net/netns/generic.h>

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Qin Fen");
MODULE_DESCRIPTION("iptables module for outbound/inbound table");
MODULE_ALIAS("ipt_fqrouter");

// state registered to net namespace as generic net pointer
static int fqrouter_net_id __read_mostly;
struct fqrouter_net {
	struct xt_table		*outbound_table;
};

// outbound
static struct nf_hook_ops *outbound_table_hook_ops __read_mostly;
#define OUTBOUND_VALID_HOOKS (1 << NF_INET_POST_ROUTING)

static const struct xt_table outbound_table_def = {
	.name		= "outbound",
	.valid_hooks	= OUTBOUND_VALID_HOOKS,
	.me		= THIS_MODULE,
	.af		= NFPROTO_IPV4,
	.priority	= NF_IP_PRI_LAST,
};

static unsigned int
outbound_table_hook(unsigned int hook,
		     struct sk_buff *skb,
		     const struct net_device *in,
		     const struct net_device *out,
		     int (*okfn)(struct sk_buff *))
{
    struct fqrouter_net *fqn;

    fqn = (struct netns_fqrouter *)net_generic(dev_net(out), fqrouter_net_id);
	return ipt_do_table(skb, hook, in, out, fqn->outbound_table);
}

// init/exit pairs
static int __net_init fqrouter_net_init(struct net *net)
{
    struct fqrouter_net *fqn;
	struct ipt_replace *repl;

    fqn = (struct fqrouter_net *)net_generic(net, fqrouter_net_id);
	repl = ipt_alloc_initial_table(&outbound_table_def);
	if (repl == NULL)
		return -ENOMEM;
	fqn->outbound_table =
		ipt_register_table(net, &outbound_table_def, repl);
	kfree(repl);
	if (IS_ERR(fqn->outbound_table))
		return PTR_ERR(fqn->outbound_table);
	return 0;
}

static void __net_exit fqrouter_net_exit(struct net *net)
{
    struct fqrouter_net *fqn;

    fqn = (struct fqrouter_net *)net_generic(net, fqrouter_net_id);
	ipt_unregister_table(net, fqn->outbound_table);
}

static struct pernet_operations fqrouter_net_ops = {
	.init = fqrouter_net_init,
	.exit = fqrouter_net_exit,
	.id   = &fqrouter_net_id,
	.size = sizeof(struct fqrouter_net),
};

static int __init xt_fqrouter_init(void)
{
	int ret;

	ret = register_pernet_subsys(&fqrouter_net_ops);
	if (ret < 0)
		return ret;

	/* Register hooks */
	outbound_table_hook_ops = xt_hook_link(&outbound_table_def, outbound_table_hook);
	if (IS_ERR(outbound_table_hook_ops)) {
		ret = PTR_ERR(outbound_table_hook_ops);
		goto cleanup_table;
	}

	return ret;

 cleanup_table:
	unregister_pernet_subsys(&fqrouter_net_ops);
	return ret;
}

static void __exit xt_fqrouter_exit(void)
{
	xt_hook_unlink(&outbound_table_def, outbound_table_hook_ops);
	unregister_pernet_subsys(&fqrouter_net_ops);
}

module_init(xt_fqrouter_init);
module_exit(xt_fqrouter_exit);
