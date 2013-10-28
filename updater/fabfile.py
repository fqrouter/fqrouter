from fabric import api
import os

def deploy():
    api.put(os.path.join(os.path.dirname(__file__), 'deploy.py'), '~/deploy.py')
    api.run('python ~/deploy.py %s' % api.env.host)
    api.run('killall ss-server', warn_only=True)
    api.run('nohup service shadowsocks start')
    for i in range(1):
        index = i + 1
        api.run('nohup ss-server -c /etc/shadowsocks/config%s.json -f /var/run/shadowsocks/shadowsocks%s.pid' % (index, index))