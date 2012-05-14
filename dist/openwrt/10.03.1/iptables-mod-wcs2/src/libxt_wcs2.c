/*
 * Shared library add-on to iptables to add wcs2 target support.
 *
 * Copyright (C) 2010 Mike Chen
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 */

#include <netinet/in.h>
#include <linux/netfilter.h>
#include <stdio.h>
#include <linux/netfilter/x_tables.h>
#include <xtables.h>
#include <stdio.h>
#include <getopt.h>
#include <string.h>
#include <stdlib.h>

#include "xt_wcs2.h"

static void xt_wcs2_tg_help(void)
{
	printf(
"wcs2 target options:\n"
"  --gateway                The target host to redirect traffic. \n"
"  --udp-port               The UDP port wcs2_server listens on. \n"
"  --xor                    Use logical xor to encrypt packet. 1 ~255 \n"
"\n");
}

static const struct option xt_wcs2_tg_opts[] = {
	{ .name = "gateway", .has_arg = 1, .val = '1'},
	{ .name = "xor", .has_arg = 1, .val = '2'},
	{ .name = "udp-port", .has_arg = 1, .val = '3'},
	{NULL},
};

static int xt_wcs2_tg_parse(int c, char **argv, int invert, unsigned int *flags,
			const void *entry, struct xt_entry_target **target)
{
	struct xt_wcs2_tg_info *tg_info = (void *)(*target)->data;

	switch (c) {
		case '1': /* --gateway */
		{
			if (invert) {
				xtables_error(PARAMETER_PROBLEM, "Unexpected `!' after --gateway");
				return XT_WCS2_PARSE_ERROR;
			}
			
			if (*flags & XT_WCS2_PARAM_GATEWAY) {
				xtables_error(PARAMETER_PROBLEM, "Multiple --gateway not supported");
				return XT_WCS2_PARSE_ERROR;
			}

			struct in_addr *addr;
			addr = xtables_numeric_to_ipaddr(optarg);
			if (!addr) {
				xtables_error(PARAMETER_PROBLEM, "Bad IP address \"%s\"\n", optarg);
				return XT_WCS2_PARSE_ERROR;
			}
			memcpy(&tg_info->gateway.in, addr, sizeof(*addr));
			*flags |= XT_WCS2_PARAM_GATEWAY;
			return XT_WCS2_PARSE_OK;
		}
		case '2': /* --xor */
			if (invert) {
				xtables_error(PARAMETER_PROBLEM, "Unexpected `!' after --xor");
				return XT_WCS2_PARSE_ERROR;
			}

			if (*flags & XT_WCS2_PARAM_XOR) {
				xtables_error(PARAMETER_PROBLEM, "Multiple --xor not supported");
				return XT_WCS2_PARSE_ERROR;
			}
			if (atoi(optarg) < 0 || atoi(optarg) > 255) {
				xtables_error(PARAMETER_PROBLEM, "--xor option out of range");
				return XT_WCS2_PARSE_ERROR;
			}
			if (atoi(optarg) == 0) {
				xtables_error(PARAMETER_PROBLEM, "--xor 0 is meaningless");
				return XT_WCS2_PARSE_ERROR;
			}

			*flags += XT_WCS2_PARAM_XOR;
			tg_info->xor_key = atoi(optarg);

			return XT_WCS2_PARSE_OK;
		case '3': /* --udp-port */
		{
			if (invert) {
				xtables_error(PARAMETER_PROBLEM, "Unexpected `!' after --udp-port");
				return XT_WCS2_PARSE_ERROR;
			}
			
			if (*flags & XT_WCS2_PARAM_UDP_PORT) {
				xtables_error(PARAMETER_PROBLEM, "Multiple --udp-port not supported");
				return XT_WCS2_PARSE_ERROR;
			}
			int t_port = atoi(optarg);
			if (t_port < 1 || t_port > 65536) {
				xtables_error(PARAMETER_PROBLEM, "Port number should be between 1~65536");
				return XT_WCS2_PARSE_ERROR;
			}
			tg_info->udp_port = t_port;

			*flags += XT_WCS2_PARAM_UDP_PORT;
			return XT_WCS2_PARSE_OK;
		}
	}
	return 0;
}

static void xt_wcs2_tg_check(unsigned int flags)
{
	if (!(flags & XT_WCS2_PARAM_GATEWAY)) {
		xtables_error(PARAMETER_PROBLEM, "wcs2 target: Parameter --gateway is required");
	}
	if (!(flags & XT_WCS2_PARAM_UDP_PORT)) {
		xtables_error(PARAMETER_PROBLEM, "wcs2 target: Parameter --udp-port is required");
	}
}

static void xt_wcs2_tg_save(const void *ip, const struct xt_entry_target *target)
{
	const struct xt_wcs2_tg_info *info = (const void *)target->data;
	printf("--gateway %s ", xtables_ipaddr_to_numeric(&info->gateway.in));
	printf("--udp-port %i ", info->udp_port);
	if (info->xor_key != 0) {
		printf("--xor %i ", info->xor_key);
	}
}

static void xt_wcs2_tg_init(struct xt_entry_target *target) {
	struct xt_wcs2_tg_info *info = (void *)target->data; 
	
	info->xor_key = 0;
}

static struct xtables_target xt_wcs2_tg_reg = {
	.name	       = "wcs2",
	.family	       = NFPROTO_IPV4,
	.version       = XTABLES_VERSION,
	.revision      = 0,
	.size	       = XT_ALIGN(sizeof(struct xt_wcs2_tg_info)),
	.userspacesize = XT_ALIGN(sizeof(struct xt_wcs2_tg_info)),
	.help	       = xt_wcs2_tg_help,
	.parse	       = xt_wcs2_tg_parse,
	.final_check   = xt_wcs2_tg_check,
	.init          = xt_wcs2_tg_init,
	.save	       = xt_wcs2_tg_save,
	.extra_opts    = xt_wcs2_tg_opts,	
};

void _init(void)
{
	xtables_register_target(&xt_wcs2_tg_reg);
}




