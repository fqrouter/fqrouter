/*
 * Server-side implementation of west-chamber-season-2. 
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

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <getopt.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <signal.h>

#define BUFLEN 2048


enum {
	WCS2_ARG_DAEMON = 1 << 0,
	WCS2_ARG_PORT = 1 << 1,
	WCS2_ARG_ADDRESS = 1 << 2,
	WCS2_ARG_XOR = 1 << 3,
};

static int udpsock, rawsock; 

static void signal_handler(int signum) {
	if (udpsock != 0) {
		close(udpsock);
		udpsock = 0;
	}
	if (rawsock != 0) {
		close(rawsock);
		rawsock = 0;
	}
	exit(0);
}

static void print_help(const char* app)
{
	printf("Usage: %s [options]\n"
		   "Options are:\n"
		   "-p <port number>    Listen on specific UDP port\n"
		   "-b <IP address>     Bind specific IP address\n"
		   "-d                  Fork off as a daemon\n"
		   "-h                  Print this help text\n"
		   "-x <1~255>		XOR key\n"
		   "\n", app);
}

int main(int argc, char* argv[]) {
	struct sockaddr_in si_me, si_user, si_target;
	int c, port, key;
	int flags;
	uint8_t xor_key;
	
	flags = 0; 
	udpsock = 0;
	memset((char*)&si_me, 0, sizeof(si_me));
	signal(SIGINT, signal_handler);
	
	while ((c = getopt(argc, argv, "hdp:b:x:")) != -1) {
		switch (c)
		{
			case 'b':
				if (inet_aton(optarg, &si_me.sin_addr) == 0) {
					fprintf(stderr, "Invalid IP address %s.\n", optarg);
					return 1;
				}
				flags |= WCS2_ARG_ADDRESS;
				break;
			case 'd':
				flags |= WCS2_ARG_DAEMON;
				break;
			case 'x':
				key = atoi(optarg);
				if (key < 1 || key > 255) {
					fprintf(stderr, "Invalid xor key %i.\n", key);
					return 1;
				}
				xor_key = (uint8_t)key;
				flags |= WCS2_ARG_XOR;
				break;
			case 'p':
				port = atoi(optarg);
				if (port < 1 || port > 65536) {
					fprintf(stderr, "Port number should be between 1~65536.\n");
					return 1;
				}
				flags |= WCS2_ARG_PORT;
				break;
			case 'h':
				print_help(argv[0]);
				return 0;
				break;
			case '?':
				if (optopt == 'p' || optopt == 'b') {
					return 1;
				}
				else {
					fprintf(stderr, "Unknown option `-%c'.\n", optopt);
					print_help(argv[0]);
					return 1;
				}
				break;
		}
	}
	
	if (!(flags & WCS2_ARG_PORT)) {
		fprintf(stderr, "You must specify a port with options '-p'.\n");
		return 1;
	}
	if (flags | WCS2_ARG_ADDRESS) {
		si_me.sin_addr.s_addr = htonl(INADDR_ANY);
	}
	
	si_me.sin_family = AF_INET;
	si_me.sin_port = htons(port);
	
	/* Create raw socket. */
	if ((rawsock = socket(AF_INET, SOCK_RAW, IPPROTO_RAW)) == -1) {
		fprintf(stderr, "raw socket() error: %s\n", strerror(errno));
		return 1;
	}
	
	/* Raw socket setsockopt */
	const int one = 1;
	if (setsockopt(rawsock, IPPROTO_IP, IP_HDRINCL, (char *)&one, sizeof(one)) == -1)
	{
		fprintf(stderr, " raw setsockopt() error: %s\n", strerror(errno));
		return 1;
	}
	
	/* Create UDP socket. */
	if ((udpsock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1) {
		fprintf(stderr, "socket() error: %s\n", strerror(errno));
		return 1;
	}
	
	/* Bind UDP socket. */
	if (bind(udpsock, (struct sockaddr *)&si_me, sizeof(si_me)) == -1) {
		fprintf(stderr, "bind() error: %s\n", strerror(errno));
		return 1;
	}
	
	/* Fork to background if specified. */
	if (flags & WCS2_ARG_DAEMON) {
		pid_t pid = fork();
		if (pid < 0) {
			fprintf(stderr, "fork() error: %s\n", strerror(errno));
			fprintf(stderr, "Try disabling daemon next time. \n");
			return 1;
		}
		
		if (pid > 0) {
			return 0;
		}
	}
	
	si_target.sin_family = AF_INET;
	
	/* Now start looping. */
	char buf[BUFLEN];
	uint32_t si_user_len = sizeof(si_user);
	int packet_len;
	while (1) {
		if ((packet_len = recvfrom(udpsock, buf, BUFLEN, 0, (struct sockaddr *)&si_user, &si_user_len)) == -1) {
			fprintf(stderr, "recvfrom() error: %s\n", strerror(errno));	
			break;
		}
		else {
			printf("wcs2_server: Received packet with total size: %i\n", packet_len);
			
			/* Decrypt if needed. */
			if (flags & WCS2_ARG_XOR) {	
				for (int i = 0; i < packet_len; i++) {
					buf[i] ^= xor_key;
				}
			}
			
			si_target.sin_addr.s_addr = *(unsigned int *)&buf[16];
			sendto(rawsock, buf, packet_len, 0, (struct sockaddr *)&si_target, sizeof(si_target));
			
		}
	}
	
	close(udpsock);
	
	return 0;
}
