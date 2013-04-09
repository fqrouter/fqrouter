CONF_BASE = """
base {
	// debug: connection progress & client list on SIGUSR1
	log_debug = on;

	// info: start and end of client session
	log_info = on;

	/* possible `log' values are:
	 *   stderr
	 *   "file:/path/to/file"
	 *   syslog:FACILITY  facility is any of "daemon", "local0"..."local7"
	 */
	log = "stderr";
	// log = "file:/path/to/file";
	// log = "syslog:local7";

	// detach from console
	daemon = off;

	/* Change uid, gid and root directory, these options require root
	 * privilegies on startup.
	 * Note, your chroot may requre /etc/localtime if you write log to syslog.
	 * Log is opened before chroot & uid changing.
	 */
	// user = nobody;
	// group = nobody;
	// chroot = "/var/chroot";

	/* possible `redirector' values are:
	 *   iptables   - for Linux
	 *   ipf        - for FreeBSD
	 *   pf         - for OpenBSD
	 *   generic    - some generic redirector that MAY work
	 */
	redirector = iptables;
}
"""

CONF_REDSOCKS = """
redsocks {
	/* `local_ip' defaults to 127.0.0.1 for security reasons,
	 * use 0.0.0.0 if you want to listen on every interface.
	 * `local_*' are used as port to redirect to.
	 */
	local_ip = 0.0.0.0;
	local_port = %s;

	// listen() queue length. Default value is SOMAXCONN and it should be
	// good enough for most of us.
	// listenq = 128; // SOMAXCONN equals 128 on my Linux box.

	// `max_accept_backoff` is a delay to retry `accept()` after accept
	// failure (e.g. due to lack of file descriptors). It's measured in
	// milliseconds and maximal value is 65535. `min_accept_backoff` is
	// used as initial backoff value and as a damper for `accept() after
	// close()` logic.
	// min_accept_backoff = 100;
	// max_accept_backoff = 60000;



	// known types: socks4, socks5, http-connect, http-relay
	type = %s;
	// `ip' and `port' are IP and tcp-port of proxy-server
	// You can also use hostname instead of IP, only one (random)
	// address of multihomed host will be used.
	ip = %s;
	port = %s;
	%s
	%s
}
"""


def render(proxies):
    sections = [CONF_BASE]
    for proxy in proxies:
        proxy_type, ip, port, username, password = proxy['connection_info']
        username = 'login = "%s";' % username if username else ''
        password = 'password = "%s";' % password if password else ''
        sections.append(CONF_REDSOCKS % (proxy['local_port'], proxy_type, ip, port, username, password))
    return ''.join(sections)