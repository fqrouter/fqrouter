module('luci.controller.fqrouter.network', package.seeall)

function index()
    entry({'fqrouter','network'}, call('network_status'), 'Network Status', 10).dependent=false
end

function network_status()
    luci.http.prepare_content("text/html")
    local netm = require "luci.model.network".init()
    local lan = netm:get_network('lan')
    luci.http.write(require 'luci.template'.render('fqrouter/network', {lan=lan}))
end