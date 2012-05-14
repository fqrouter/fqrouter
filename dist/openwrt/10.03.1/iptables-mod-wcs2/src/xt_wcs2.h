#ifndef _XT_VROUTE_H_target
#define _XT_VROUTE_H_target

/* Data structure supports IPv6. */
struct xt_wcs2_tg_info {
	union nf_inet_addr gateway; 	/* Virtual gateway. */
	__u16 udp_port; 	/* The UDP port the virtual gateway listens on. */
	__u8 xor_key; 	/* Encrytion key, ignore when type is none. */
}; 

#define XT_WCS2_PARSE_OK	1
#define XT_WCS2_PARSE_ERROR	0

enum {
	XT_WCS2_PARAM_GATEWAY = 1 << 0,
	XT_WCS2_PARAM_XOR = 1 << 1,
	XT_WCS2_PARAM_UDP_PORT = 1 << 2,
};

#endif /* _XT_VROUTE_H_target */

