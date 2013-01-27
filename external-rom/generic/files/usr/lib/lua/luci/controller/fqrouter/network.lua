module('luci.controller.fqrouter.network', package.seeall)

function index()
    entry({ 'fqrouter', 'network', 'status' }, call('network_status'), 'Network Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'status' }, call('upstream_status'), 'Upstream Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'internet', 'status' }, call('internet_status'), 'Internet Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'list' }, call('upstream_list'), 'Upstream List', 10).dependent = false
    entry({ 'fqrouter', 'network', 'ping.js' }, call('network_ping'), 'Network Ping', 10).dependent = false
    entry({ 'fqrouter', 'network', 'lan_ip', 'update' }, call('update_lan_ip'), 'Update Lan IP', 10).dependent = false
    entry({ 'fqrouter', 'network', 'restart' }, call('restart_network'), 'Restart Network', 10).dependent = false
end

function network_status()
    luci.http.prepare_content('text/html')
    require'luci.template'.render('fqrouter/network/status', {
        upstream_status_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'status'),
        upstream_list_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'list'),
        internet_status_url = luci.dispatcher.build_url('fqrouter', 'network', 'internet', 'status')
    })
end

function upstream_status()
    local netm = require'luci.model.network'.init()
    local upstream_status = '没有与上级无线连接好'
    for i, network in ipairs(netm:get_networks()) do
        if network:gwaddr() then
            upstream_status = '已经连上了上级无线'
        end
    end
    luci.http.prepare_content('text/plain')
    luci.http.write(upstream_status)
end

function internet_status()
    local internet_status = '没有与互联网连接好'
    local f = assert(io.popen('ping 8.8.8.8 -c 1'))
    local output = assert(f:read('*a'))
    if output:find('1 packets received') then
        internet_status = '已经连上了互联网'
    end
    luci.http.prepare_content('text/plain')
    luci.http.write(internet_status)
end

function upstream_list()
    local iw = require'luci.sys'.wifi.getiwinfo('radio0')
    local upstream_list = {}
    local no_upstream_found = true
    for k, v in ipairs(iw.scanlist or { }) do
        upstream_list[v.ssid] = {
            signal_strength=calculate_signal_strength(v),
            bssid=v.bssid,
            encryption=get_encryption(v),
            is_configured=false
        }
        no_upstream_found = false
    end
    function set_is_configured_flag(section)
        if 'sta' == section.mode and upstream_list[section.ssid] ~= nil then
            upstream_list[section.ssid].is_configured=true
        end
    end
    x = require'uci'.cursor()
    x:foreach('wireless', 'wifi-iface', set_is_configured_flag)
    require'luci.template'.render('fqrouter/network/upstream-list', {
        upstream_list = upstream_list,
        no_upstream_found=no_upstream_found
    })
end

function get_encryption(info)
    if info.encryption.wpa > 0 then
        return info.encryption.wpa >= 2 and 'psk2' or 'psk'
    else
        return nil
    end
end

function calculate_signal_strength(info)
    local qc = info.quality or 0
    local qm = info.quality_max or 0

    if info.bssid and qc > 0 and qm > 0 then
        return math.floor((100 / qm) * qc)
    else
        return 0
    end
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