from fabric import api
import os

def all():
    api.env.hosts = [
        # 'root@174.140.169.55',
        # 'root@174.140.171.244',
        # 'root@199.188.75.60',
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
    ]

def deploy():
    api.put(os.path.join(os.path.dirname(__file__), 'deploy.py'), '~/deploy.py')
    api.run('python ~/deploy.py %s' % api.env.host)
    api.run('killall ss-server', warn_only=True)
    api.run('nohup service shadowsocks start')
    for i in range(3):
        index = i + 1
        api.run('nohup ss-server -c /etc/shadowsocks/config%s.json -f /var/run/shadowsocks/shadowsocks%s.pid' % (index, index))

def block_80():
    api.run('iptables --flush')
    api.run('iptables -I INPUT -d %s -p tcp --dport 3000 -j DROP' % api.env.host)
    api.run('iptables -I OUTPUT -s %s -p tcp --dport 80 -j DROP' % api.env.host)