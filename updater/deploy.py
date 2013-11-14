import sys
import os
import shutil

ip = sys.argv[1]

if not os.path.exists('/usr/bin/ss-server.orig'):
    shutil.move('/usr/bin/ss-server', '/usr/bin/ss-server.orig')
    with open('/usr/bin/ss-server', 'w') as f:
        f.write(
"""#!/bin/sh
ulimit -n 8000
exec /usr/bin/ss-server.orig $@
""")
    os.chmod('/usr/bin/ss-server', 0755)

with open('/etc/shadowsocks/config.json', 'w') as f:
    f.write(
        """
{
    "server":"%s",
    "server_port":220,
    "local_port":1080,
    "password":"A76nOIHdZEYw",
    "timeout":60,
    "method":"rc4"
}
        """ % ip)

for i in range(1):
    index = i + 1
    with open('/etc/shadowsocks/config%s.json' % index, 'w') as f:
        f.write(
            """
    {
        "server":"%s",
        "server_port":22%s,
        "local_port":108%s,
        "password":"A76nOIHdZEYw",
        "timeout":60,
        "method":"rc4"
    }
            """ % (ip, index, index))