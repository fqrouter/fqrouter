module('luci.controller.fqrouter.network', package.seeall)

function index()
    entry({ 'fqrouter', 'network', 'status' }, call('network_status'), 'Network Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'status' }, call('upstream_status'), 'Upstream Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'internet', 'status' }, call('internet_status'), 'Internet Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'ping.js' }, call('network_ping'), 'Network Ping', 10).dependent = false
    entry({ 'fqrouter', 'network', 'lan_ip', 'update' }, call('update_lan_ip'), 'Update Lan IP', 10).dependent = false
    entry({ 'fqrouter', 'network', 'restart' }, call('restart_network'), 'Restart Network', 10).dependent = false
end

function network_status()
    luci.http.prepare_content('text/html')
    require'luci.template'.render('fqrouter/network/status', {
        upstream_status_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'status'),
        internet_status_url = luci.dispatcher.build_url('fqrouter', 'network', 'internet', 'status')
    })
end

function upstream_status()
    local netm = require'luci.model.network'.init()
    local upstream_status = '没有与上级无线连接好'
    for i, network in ipairs(netm:get_networks()) do
        if network:gwaddr() then
            upstream_status = '已经连接上了上级无线，ip是：' .. network:ipaddr()
        end
    end
    luci.http.prepare_content('text/plain')
    luci.http.write(upstream_status)
end

function internet_status()
    local f = assert(io.popen('wget http://www.ifconfig.me/all.json -q -O -'))
    local output = assert(f:read('*a'))
    luci.http.write(output)
end

function network_ping()
    luci.http.prepare_content('application/javascript')
    luci.http.write('window.pong();')
end

function update_lan_ip()
    local old_lan_ip = require'luci.model.network'.init():get_network('lan'):ipaddr()
    local new_lan_ip = luci.http.formvalue('lan_ip')
    x = require'uci'.cursor()
    x:set('network', 'lan', 'ipaddr', new_lan_ip)
    x:commit('network')
    require'luci.template'.render('fqrouter/network/restart', {
        notice = '因为改变了路由器IP地址，请先手工断开与路由器的连接，再重新连接。',
        restart_url = 'http://' .. old_lan_ip .. luci.dispatcher.build_url('fqrouter', 'network', 'restart'),
        status_url = 'http://' .. new_lan_ip .. luci.dispatcher.build_url('fqrouter', 'network', 'status'),
        ping_url = 'http://' .. new_lan_ip .. luci.dispatcher.build_url('fqrouter', 'network', 'ping.js')
    })
end

function restart_network()
    os.execute('/etc/init.d/network restart')
end