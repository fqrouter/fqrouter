module('luci.controller.fqrouter.network', package.seeall)

function index()
    local root = node()
    root.target = alias('fqrouter', 'network', 'status')
    root.index = true
    entry({ 'fqrouter', 'network', 'status' }, call('network_status'), 'Network Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'internet', 'status' }, call('internet_status'), 'Internet Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'status' }, call('upstream_status'), 'Upstream Status', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'list' }, call('upstream_list'), 'Upstream List', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'config' }, call('upstream_config'), 'Upstream Config', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'connect' }, call('connect_upstream'), 'Connect Upstream', 10).dependent = false
    entry({ 'fqrouter', 'network', 'upstream', 'delete' }, call('delete_upstream'), 'Delete Upstream', 10).dependent = false
    entry({ 'fqrouter', 'network', 'lan-ip', 'config' }, call('lan_ip_config'), 'Lan IP Config', 10).dependent = false
    entry({ 'fqrouter', 'network', 'lan-ip', 'update' }, call('update_lan_ip'), 'Update Lan IP', 10).dependent = false
    entry({ 'fqrouter', 'network', 'restart' }, call('restart_network'), 'Restart Network', 10).dependent = false
    entry({ 'fqrouter', 'network', 'ping.js' }, call('network_ping'), 'Network Ping', 10).dependent = false
end

function network_status()
    luci.http.prepare_content('text/html')
    require'luci.template'.render('fqrouter/network/status', {
        upstream_status_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'status'),
        upstream_list_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'list'),
        internet_status_url = luci.dispatcher.build_url('fqrouter', 'network', 'internet', 'status'),
        restart_url = luci.dispatcher.build_url('fqrouter', 'network', 'restart')
    })
end

function internet_status()
    local internet_status = 'NOT_CONNECTED'
    local f = assert(io.popen('ping 8.8.8.8 -c 1'))
    local output = assert(f:read('*a'))
    if output:find('1 packets received') then
        internet_status = 'CONNECTED'
    end
    luci.http.prepare_content('text/plain')
    luci.http.write(internet_status)
end

function upstream_status()
    local netm = require'luci.model.network'.init()
    local lan_ip = netm:get_network('lan'):ipaddr()
    local lan_ip_part1, lan_ip_part2, lan_ip_part3 = lan_ip:match('(%d%d?%d?)%.(%d%d?%d?)%.(%d%d?%d?)%.%d%d?%d?')
    local upstream_status = 'NOT_CONNECTED'
    for i, network in ipairs(netm:get_networks()) do
        if network:gwaddr() then
            local wan_ip = network:ipaddr()
            local wan_ip_part1, wan_ip_part2, wan_ip_part3 = wan_ip:match('(%d%d?%d?)%.(%d%d?%d?)%.(%d%d?%d?)%.%d%d?%d?')
            if lan_ip_part1 == wan_ip_part1 and lan_ip_part2 == wan_ip_part2 and lan_ip_part3 == wan_ip_part3 then
                upstream_status = 'CONFLICT_IP_DETECTED'
            else
                upstream_status = 'CONNECTED'
            end
        end
    end
    if 'NOT_CONNECTED' == upstream_status then
        local error_file = io.open('/tmp/upstream-status', 'r')
        if error_file ~= nil then
            upstream_status = error_file:read('*all')
            io.close(error_file)
        end
    end
    luci.http.prepare_content('text/plain')
    luci.http.write(upstream_status)
end

function upstream_list()
    local iw = require'luci.sys'.wifi.getiwinfo('radio0')
    local upstream_list = {}
    local remote_saved_upstream_list = {}
    local no_upstream_available = true
    for k, v in ipairs(iw.scanlist or {}) do
        upstream_list[v.ssid] = {
            signal_strength = calculate_signal_strength(v),
            bssid = v.bssid,
            encryption = get_encryption(v),
            password = nil
        }
        no_upstream_available = false
    end
    function add_saved_upstream(section)
        if 'sta' == section.mode then
            if upstream_list[section.ssid] == nil then
                table.insert(remote_saved_upstream_list, {
                    ssid = section.ssid,
                    bssid = section.bssid,
                    encryption = section.encryption,
                    password = section.password
                })
            else
                upstream_list[section.ssid].password = section.key
            end
        end
    end

    x = require'uci'.cursor()
    x:foreach('wireless', 'wifi-iface', add_saved_upstream)
    require'luci.template'.render('fqrouter/network/upstream-list', {
        upstream_list = upstream_list,
        remote_saved_upstream_list=remote_saved_upstream_list,
        no_upstream_available = no_upstream_available,
        connect_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'connect'),
        configure_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'config')
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

function upstream_config()
    local ssid = luci.http.formvalue('ssid')
    local bssid = luci.http.formvalue('bssid')
    local encryption = luci.http.formvalue('encryption')
    local password = luci.http.formvalue('password')
    require'luci.template'.render('fqrouter/network/upstream-config', {
        ssid = ssid,
        bssid = bssid,
        encryption = encryption,
        password = password,
        connect_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'connect'),
        delete_url = luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'delete')
    })
end

function connect_upstream()
    local ssid = luci.http.formvalue('ssid')
    local bssid = luci.http.formvalue('bssid')
    local encryption = luci.http.formvalue('encryption')
    local password = luci.http.formvalue('password')

    x = require'uci'.cursor()
    function delete_existing(section)
        if ssid == section.ssid then
            x:delete('wireless', section['.name'])
        end
    end

    x:foreach('wireless', 'wifi-iface', delete_existing)
    local section_name = x:add('wireless', 'wifi-iface')
    x:set('wireless', section_name, 'network', 'wan')
    x:set('wireless', section_name, 'ssid', ssid)
    x:set('wireless', section_name, 'encryption', encryption)
    x:set('wireless', section_name, 'device', 'radio0')
    x:set('wireless', section_name, 'mode', 'sta')
    x:set('wireless', section_name, 'bssid', bssid)
    x:set('wireless', section_name, 'key', password)
    x:commit('wireless')
    luci.http.redirect(luci.dispatcher.build_url('fqrouter', 'network', 'restart'))
end

function delete_upstream()
    local ssid = luci.http.formvalue('ssid')
    x = require'uci'.cursor()
    function delete_existing(section)
        if ssid == section.ssid then
            x:delete('wireless', section['.name'])
        end
    end

    x:foreach('wireless', 'wifi-iface', delete_existing)
    x:commit('wireless')
    luci.http.redirect(luci.dispatcher.build_url('fqrouter', 'network', 'upstream', 'list'))
end

function lan_ip_config()
    local netm = require'luci.model.network'.init()
    local lan_ip = netm:get_network('lan'):ipaddr()
    require'luci.template'.render('fqrouter/network/lan-ip-config', {
        lan_ip = lan_ip,
        update_url = luci.dispatcher.build_url('fqrouter', 'network', 'lan-ip', 'update')
    })
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
    if 'POST' == luci.http.getenv().REQUEST_METHOD then
        os.execute('echo "RESTARTING" > /tmp/upstream-status && '
            ..'/etc/init.d/network restart && sleep 1 && '
            ..'/etc/init.d/dnsmasq restart && sleep 1 && '
            ..'/etc/init.d/firewall restart')
    else
        require'luci.template'.render('fqrouter/network/restart', {
            restart_url = luci.dispatcher.build_url('fqrouter', 'network', 'restart'),
            status_url = luci.dispatcher.build_url('fqrouter', 'network', 'status'),
            ping_url = luci.dispatcher.build_url('fqrouter', 'network', 'ping.js')
        })
    end
end

function network_ping()
    luci.http.prepare_content('application/javascript')
    luci.http.write('window.pong();')
end