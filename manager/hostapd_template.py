TEMPLATE = """
#
# hostapd.conf
#
# hostapd configuration file template
#
# Copyright (C) {2011} Texas Instruments Incorporated - http://www.ti.com/
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

driver=nl80211
logger_syslog=-1
logger_syslog_level=0
logger_stdout=-1
logger_stdout_level=0
dump_file=/data/misc/wifi/hostapd.dump
ctrl_interface=%s
hw_mode=g
channel=%s
beacon_int=100
dtim_period=2
supported_rates=10 20 55 110 60 90 120 180 240 360 480 540
basic_rates=60 120 240
preamble=1
ignore_broadcast_ssid=0
wpa_group_rekey=0
wpa_gmk_rekey=0
wpa_ptk_rekey=0
interface=ap0
auth_algs=3
wpa=2
wpa_key_mgmt=WPA-PSK
wpa_pairwise=CCMP
ssid=%s
wpa_passphrase=%s
ieee80211d=1
country_code=US
max_num_sta=8
macaddr_acl=0
tx_queue_data3_aifs=7
tx_queue_data3_cwmin=15
tx_queue_data3_cwmax=1023
tx_queue_data3_burst=0
tx_queue_data2_aifs=3
tx_queue_data2_cwmin=15
tx_queue_data2_cwmax=63
tx_queue_data2_burst=0
tx_queue_data1_aifs=1
tx_queue_data1_cwmin=7
tx_queue_data1_cwmax=15
tx_queue_data1_burst=3.0
tx_queue_data0_aifs=1
tx_queue_data0_cwmin=3
tx_queue_data0_cwmax=7
tx_queue_data0_burst=1.5
wme_enabled=1
wme_ac_bk_cwmin=4
wme_ac_bk_cwmax=10
wme_ac_bk_aifs=7
wme_ac_bk_txop_limit=0
wme_ac_bk_acm=0
wme_ac_be_aifs=3
wme_ac_be_cwmin=4
wme_ac_be_cwmax=10
wme_ac_be_txop_limit=0
wme_ac_be_acm=0
wme_ac_vi_aifs=2
wme_ac_vi_cwmin=3
wme_ac_vi_cwmax=4
wme_ac_vi_txop_limit=94
wme_ac_vi_acm=0
wme_ac_vo_aifs=2
wme_ac_vo_cwmin=2
wme_ac_vo_cwmax=3
wme_ac_vo_txop_limit=47
wme_ac_vo_acm=0
uapsd_advertisement_enabled=1
wep_rekey_period=0
own_ip_addr=127.0.0.1
wpa_group_rekey=0
wpa_gmk_rekey=0
wpa_ptk_rekey=0
ap_table_max_size=255
ap_table_expiration_time=60
wps_state=2
ap_setup_locked=1
uuid=12345678-9abc-def0-1234-56789abcdef0
eap_server=1
disassoc_low_ack=1
ap_max_inactivity=10000
ieee80211n=1
# information elements
device_name=Wireless AP
manufacturer=TexasInstruments
model_name=TI_Connectivity_module
model_number=wl12xx
serial_number=12345
device_type=0-00000000-0
"""


def render(iface, channel, ssid, password):
    return TEMPLATE % (iface, channel, ssid, password)