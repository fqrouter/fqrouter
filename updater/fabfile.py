from fabric import api
import os

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