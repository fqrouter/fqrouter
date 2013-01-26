module('luci.controller.fqrouter.network', package.seeall)

function index()
    entry({'fqrouter','network', 'status'}, call('network_status'), 'Network Status', 10).dependent=false
    entry({'fqrouter','network', 'lan_ip', 'update'}, call('update_lan_ip'), 'Update Lan IP', 10).dependent=false
end

function network_status()
    luci.http.prepare_content("text/html")
    local netm = require "luci.model.network".init()
    local lan = netm:get_network('lan')
    luci.http.write(require 'luci.template'.render('fqrouter/network', {lan=lan}))
end

function update_lan_ip()
    luci.http.write_json(luci.http.formvaluetable())
end