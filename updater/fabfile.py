from fabric import api
import os

def all():
    api.env.hosts = [
        'root@174.140.169.55',
        'root@174.140.171.244',
        'root@199.188.75.60',
        'root@192.184.80.11',
        'root@176.56.236.63',
        'root@162.220.11.191',
        'root@23.226.131.18',
        'root@209.141.57.22',
        'root@174.140.169.62',
        'root@174.140.169.65',
        'root@162.217.248.65',
        'root@162.217.248.91',
        'root@162.220.240.220',
        'root@192.249.61.233',
        'root@198.98.49.121',
        'root@192.184.94.236',
        'root@208.117.11.211',
        'dejavu@192.227.168.26'
    ]

def deploy():
    api.put(os.path.join(os.path.dirname(__file__), 'deploy.py'), '~/deploy.py')
    api.sudo('python ~/deploy.py %s' % api.env.host)
    api.sudo('killall ss-server', warn_only=True)
    api.sudo('nohup service shadowsocks start')
    for i in range(1):
        index = i + 1
        api.sudo('nohup ss-server -c /etc/shadowsocks/config%s.json -f /var/run/shadowsocks/shadowsocks%s.pid' % (index, index))

def iptables():
    api.sudo('iptables --flush')
    api.sudo('iptables -A OUTPUT -s %s -d 173.194.0.0/16 -p tcp --dport 80 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -d 74.125.0.0/16 -p tcp --dport 80 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --dport 443 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --sport 22 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --sport 80 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --sport 220 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --sport 221 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --sport 222 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -p tcp --sport 223 -j ACCEPT' % api.env.host)
    api.sudo('iptables -A OUTPUT -s %s -j DROP' % api.env.host)
    api.sudo('iptables -A INPUT -d %s -p tcp --dport 3000 -j DROP' % api.env.host)