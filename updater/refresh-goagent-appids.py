#!/usr/bin/env python
import sys
import os
import random

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
import gevent
import gevent.monkey
import gevent.queue
import traceback
import fqsocks.proxies.goagent
import socket
import subprocess
import time
import datetime
import logging

LOGGER = logging.getLogger(__name__)

logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)s %(message)s')

T1_APP_IDS = []
for i in range(1, 215):
    for j in range(1, 11):
        T1_APP_IDS.append('i8964-%03d-%02d' % (i, j))
for i in range(215, 317):
    for j in range(1, 11):
        T1_APP_IDS.append('iwatch-%03d-%02d' % (i, j))
for i in range(1, 1000):
    T1_APP_IDS.append('freegoagent%03d' % i)
T2_APP_IDS = ['fgabootstrap001', 'fgabootstrap002', 'fgabootstrap003', 'fgabootstrap004',
             'fgabootstrap005', 'fgabootstrap006', 'fgabootstrap007', 'fgabootstrap008', 'fgabootstrap009',
             'fgabootstrap010', 'fgaupdate001', 'fgaupdate002', 'fgaupdate003', 'fgaupdate004', 'fgaupdate005',
             'fgaupdate006', 'fgaupdate007', 'fgaupdate008', 'fgaupdate009', 'fgaupdate010', 'fganr001', 'fganr002',
             'fanyueproxy1-01', 'fanyueproxy1-02', 'fanyueproxy1-03', 'fanyueproxy1-04', 'fanyueproxy1-05'] + [
              'vi88com1', 'vi88com10', 'vi88com 11', 'vi88com2', 'vi88com12', 'vi88com3', 'vi88com13', 'vi88com4',
              'vi88com14', 'vi88com5', 'vi88com15', 'vi88com6', 'vi88com16', 'vi88com7', 'vi88com17', 'vi88com8',
              'vi88com18', 'vi88com19', 'vip6xlgonggongid01', 'gongongid02', 'gonggongid03', 'gonggongid04',
              'gonggongid05', 'gonggongid06', 'gonggongid07', 'gonggongid08', 'gonggongid09', 'gonggongid10',
              'goagent-dup001', 'goagent-dup002', 'goagent-dup003', 'gonggongid11', 'gonggongid12', 'gonggongid13',
              'gonggongid14', 'gonggongid15', 'gonggongid16', 'gonggongid17', 'gonggongid18', 'gonggongid19',
              'gonggongid20', 'gfwsbgfwsbgfwsb', '1.sbgfwsbgfwsbgfw', '1.wyq476137265', '1.wangyuqi19961213',
              'xinxijishuwyq21', 'xinxijishuwyq22', 'xinxijishuwyq23', 'xinxijishuwyq24', 'xinxijishuwyq25',
              'wuxinchengboy', 'wuxinchengforever', 'wuxinchenghappy','wuxinchengjava', 'wuxinchengjoy',
              'wuxinchenglad','wuxinchenglove','wuxinchengood', 'wuxinchengsuccessful','wuxinchengsunshine',
              'goagent-dup001', 'goagent-dup002', 'goagent-dup003', 'goagent-dup004', 'goagent-dup005',
              'goagent-dup006', 'willintown', 'webgfw', 'neewgfw', 'neeegfw', 'opluyko', 'fanyue17a', 'fanyue17b',
              'doggfw', 'fanyuegfw1', 'fanyuegfw2', 'fanyuegfw3', 'fanyuegfw4', 'fanyuegfw5', 'fanyuegfw6',
              'fanyuegfw7', 'fanyuegfw8', 'fanyuegfw9', 'fanyuegfw10', 'publicgoagent2', 'gfanqiang016-4',
              'gfanqiang024-5', 'smartladdercanada', 'gfanqiang020-2', 'wowdonate-tts-28', 'gfanqiang012-9',
              'gfanqiang028-5', 'wowdonate-ch-35', 'gfanqiang029-0', 'gfanqiang000-4', 'jpj4register3',
              'gfanqiang021-2', 'gfanqiang013-6', 'nbslf8', 'wowdonate-sd-04', 'wowdonate-ceec-03', 'lovejiani052',
              'jk9084s-03', 'gfanqiang009-9', 'sfuyong5', 'agfw-server-4', 'gfanqiang025-7', 'wowdonate-m18-04',
              'gfanqiang005-7', 'chrometieba', 'fghdis9105z-05', 'wowdonate-ch-58', 'fgabootstrap002',
              'wowdonate-hj-01', 'youxiang190007', 'gfanqiang009-0', 'gfanqiang027-1',
              'jk9084s-02', 'publicwallproxy1', 'gfanqiang031-1', 'gfanqiang036-7',
              'wowdonate-m18-06', 'gfanqiang001-3', 'gfanqiang029-2', 'gfanqiang021-3', 'lovejiani051',
              'wowdonate-ch-59', 'lovejiani038', 'wowdonate-sd-30', 'gfanqiang004-7', 'wowdonate-sd-07',
              'wowdonate-ch-18', 'wowdonate-m18-25', 'fangbingxingtodie', 'lovejiani056', 'gfanqiang023-4',
              'gfanqiang010-6', 'gfanqiang026-9', 'wowdonate-m18-70', 'goagent-dup001', 'gfanqiang000-9',
              'lovejiani034', 'gfanqiang009-3', 'gfanqiang009-8', 'gfanqiang034-1', 'wowdonate-lvp-19',
              'gfanqiang016-2', 'lovejiani024', 'wowdonate-lvp-02', 'gfanqiang034-9', 'wowdonate-sd-17',
              'kawaiiushio2', 'wowdonate-lvp-04', 'cielo032023', 'kawaiiushio1', 'kawaiiushio9', 'gfanqiang024-6',
              'youxiang012341', 'lovejiani036', 'lovejiani002', 'wowdonate-ch-55', 'gfanqiang035-7', 'wowdonate-sd-06',
              'wowdonate-100-01', 'wowdonate-sx-09', 'wowdonate-edg-05', 'gfanqiang001-0', 'wowdonate-ch-29',
              'gfanqiang000-5', 'xinxijishuwyq22', 'starballl', 'wowdonate-edg-06', 'gfanqiang006-3', 'overwallmin',
              'gonggongid05', 'chrome360q', 'gfanqiang023-8', 'vi88com5', 'starballl3', 'wowdonate-ch-62',
              'wowdonate-m18-40', 'wowdonate-tts-03', 'lovejiani044', 's-g9085h-05', 'wowdonate-sofa-03',
              'wowdonate-sofa-19', 'gfanqiang036-9', 'wowdonate-badboy-01', 'wowdonate-ch-20', 'gfanqiang028-1',
              'gfanqiang019-0', 'lovejiani004', 'gfanqiang028-0', 'x9088-sz-09', 'wowdonate-sofa-07',
              'wowdonate-ceec-01', 'lovejiani027', 'lovejiani001', 'vi88com1', 'xideassassin1', 'wowdonate-m18-15',
              'lovejiani006', 'wowdonate-m18-09', 'xinxijishuwyq25', 'gfanqiang033-5', 'wowdonate-ch-01',
              'icezero0006', 'gfanqiang029-6', 'wowdonate-m18-38', 'gfanqiang002-1', 'gfanqiang008-8',
              'gfanqiang020-9', 'wowdonate-tts-30', 'wowdonate-ceec-10', 'gfanqiang015-3', 'wowdonate-ch-69',
              'wowdonate-ch-78', 'gfanqiang026-8', 'gfanqiang020-6', 'szdsdf9104-10', 'wowdonate-ceec-07',
              'gfanqiang022-8', 'overwallxm', 'wowdonate-m18-67', 'gfanqiang030-2', 'wowdonate-sx-08',
              'gfanqiang003-8', 'gfanqiang017-5', 'xideassassin9', 'overwalltmd', 'publicgoagent5',
              'gfanqiang032-3', 'lovejiani068', 'gfanqiang003-0', 'gfanqiang003-3', 'wowdonate-sd-18',
              'gfanqiang033-1', 'gfanqiang010-8', 'gfanqiang036-5', 'fgaupdate004', 'gfanqiang014-4',
              'gfanqiang007-1', 'gfanqiang022-0', 'wowdonate-sofa-13', 'stepfordcuckoos02', 'gfanqiang008-0',
              'jk9084s-01', 'intoskip7', 'wowdonate-tts-08', 'gfanqiang010-4', 'smartladder7', 'gfanqiang033-8',
              'gfanqiang029-1', 'wowdonate-ch-79', 'lovejiani062', 'smartladder005', 'fgaupdate003',
              'gfw46795', 'smartladder2', 'wowdonate-m18-50', 'gfanqiang035-0', 'gfanqiang006-0', 'gfanqiang024-2',
              'gfanqiang012-0', 'publicgoagent7', 'wowdonate-m18-32', 'lovejiani023', 'wowdonate-lvp-10',
              'wowdonate-tts-16', 'szdsdf9104-04', 'x9088-sz-06', 'wowdonate-fuyong-01', 'sekaiwakerei',
              'wowdonate-tts-20', 'wowdonate-ch-28', 'wowdonate-m18-56', 'smartladderkoera', 'vbtcozzh',
              'gfanqiang008-3', 'chromeichi', 'kawaiiushio7', 'gfanqiang014-1', 'gfanqiang030-5', 'overwallone',
              'wowdonate-m18-37', 'gfanqiang018-1', 'gfanqiang006-7', 'wowdonate-ch-75', 'gfanqiang006-5',
              'gfanqiang035-1', 'wowdonate-ch-66', 'gfanqiang032-4', 'smartladder007', 'gfanqiang013-0',
              'gonggongid16', 'smartladder016', 'lovejiani065', 'gfanqiang009-2', 'wowdonate-m18-27',
              'gfanqiang005-4', 'wowdonate-badboy-08', 'lovejiani070', 'gfanqiang035-4', 'ttphc6', 'vi88com2',
              'lovejiani058', 'wowdonate-ch-49', 'icezero0009', 'gfanqiang026-1', 'wowdonate-lvp-03',
              'sfuyong8', 'wowdonate-m18-22', 'gonggongid17', 'youxiang01239', 'wowdonate-ch-46',
              'gfanqiang032-2', 'gfanqiang005-1', 'publicfqd', 'gfanqiang015-6', 'lovejiani031',
              'wowdonate-sofa-09', 'wowdonate-sx-01', 'cielo032018', 'wowdonate-badboy-04', 'kawaiiushio5',
              'wowdonate-tts-25', 'szdsdf9104-02', 'ftencentuck', 'banzhangzhuanyong', 'wowdonate-fuyong-04',
              'x9088-sz-04', 'nbslf9', 'gfanqiang024-4', 'gfanqiang027-4', 'pdslzq7', 'gfanqiang002-2',
              'lovejiani030', 'gfanqiang002-8', 'wowdonate-m18-39', 'wowdonate-ch-22', 'youxiang190005',
              'lovejiani019', 'lovejiani053', 'wowdonate-sx-07', 'vi88com6', 'wowdonate-ch-03', 'starballl4',
              'gfanqiang032-6', 'pinksbl7', 'wowdonate-100-09', 'wowdonate-ch-26', 'gfanqiang032-9',
              'gfwsbgfwsbgfwsb', 'gfanqiang008-5', 'fghdis9105z-04', 'gfanqiang017-4', 'wowdonate-m18-29',
              'gfanqiang030-0', 'gfanqiang022-3', 'wowdonate-fuyong-10', 'wowdonate-m18-65', 'wowdonate-ceec-09',
              'publicfqi', 'gfanqiang001-5', 'overwalls3444', 'wowdonate-sofa-15', 'wowdonate-m18-28', 'jass-button-1',
              'gfanqiang028-2', 'vi88com7', 'lovejiani050', 'wowdonate-m18-62', 'gfanqiang025-3', 'wowdonate-sd-12',
              'kawaiiushio8', 'wowdonate-sd-19', 'wowdonate-badboy-05', 'smartladderhongkong', 'wowdonate-sd-11',
              'gfanqiang026-7', 'wowdonate-ch-61', 'wowdonate-lvp-05', 'wowdonate-sd-09', 'wowdonate-badboy-07',
              'wowdonate-ch-72', 'xk8581221761', 'wowdonate-sd-35', 'wowdonate-ch-67', 'wowdonate-tts-23',
              'wowdonate-sofa-10', 'gfanqiang000-3', 'lovejiani035', 'wowdonate-lvp-16', 'jk9084s-07',
              'wowdonate-ch-09', 'smartladder001', 'kawaiiushioplus', 'wowdonate-ch-40', 'wowdonate-ch-41',
              'wowdonate-tts-29', 'wowdonate-100-10', 'gfanqiang025-2', 'youxiang01236', 'wowdonate-ch-25',
              'goagentplus001', 'wowdonate-m18-48', 'lovejiani026', 'gfanqiang020-7', 'gfanqiang003-6',
              'gfanqiang010-9', 'gfanqiang027-2', 'wowdonate-ch-14', 'wowdonate-sd-23', 'smartladder011',
              'gfw46792', 'gfanqiang023-7', 'gfanqiang034-7', 'zzhclannadvbtco', 'lovejiani060', 'wowdonate-tts-07',
              'smartladder000', 'gfanqiang022-2', 'wowdonate-m18-05', 'smartladder014', 'gfanqiang009-7',
              'wowdonate-sd-16', 'gfanqiang017-2', 'stepfordcuckoos05', 'fghdis9105z-10', 'gfanqiang013-1',
              'aitaiyokani', 'gfanqiang031-8', 'gfanqiang013-9', 'gfanqiang013-5', 'wowdonate-sd-34',
              'wowdonate-m18-60', 'wowdonate-sofa-17', 'fgabootstrap009', 'wowdonate-badboy-03', 'xideassassin5',
              'gfanqiang020-0', 'chromelucky', '1.sbgfwsbgfwsbgfw', 'gfanqiang034-6', 'wowdonate-edg-07',
              'gfanqiang007-6', 'cielo032020', 'wowdonate-hj-10', 'wowdonate-ch-02', 'starballl2', 'wowdonate-sd-15',
              'gfanqiang007-3', 'kawaiiushio', 'wowdonate-lvp-17', 'overwallthree', 'youxiang0123', 'gfanqiang033-3',
              'gfanqiang003-2', 'gfanqiang002-6', 'lovejiani032', 'yugongxisaiko', 's-g9085h-02', 'gfanqiang015-7',
              'gonggongid02', 'fgaupdate010', 'wowdonate-sd-29', 'gfanqiang020-3', 'goagent-dup003',
              'wowdonate-sofa-20', 'overwallfbx', 'gfanqiang004-1', 'wowdonate-sx-04', 'overwalljp',
              'wowdonate-100-05', 'wowdonate-tts-04', 'wowdonate-tts-21', 'lovejiani044', 'smartladder004',
              'wowdonate-sofa-06', 'gfanqiang019-1', 's-g9085h-04', 'lovejiani028', 'andyooobot', 'smartladdertaiwan',
              'wowdonate-m18-53', 'huang0zijian', 'gfanqiang033-9', 'youxiang190002', 'wowdonate-badboy-10',
              'wowdonate-sofa-04', 'lovejiani059', 'wowdonate-100-08', 'gfanqiang025-9', 'gfanqiang004-9',
              'wowdonate-fuyong-06', 'wowdonate-lvp-20', 'xinxijishuwyq21', 'wowdonate-sx-10', 'gfanqiang025-8',
              'wowdonate-sd-20', 'gfanqiang029-8', 'wowdonate-ch-65', 'lovejiani064', 's-g9085h-01', 'gfanqiang022-4',
              'gfanqiang017-8', 'vbtcoclannad', 'gfanqiang002-9', 'szdsdf9104-08', 'wowdonate-hj-02', 'gfanqiang024-3',
              'wowdonate-sd-08', 'pdslzq9', 'gfanqiang007-5', 'publicfqb', 'gfanqiang001-7', 'gfanqiang036-3',
              'wowdonate-hj-03', 'x9088-sz-05', 'fgabootstrap001', 'starballl7', 'gfanqiang008-6', 'wowdonate-sx-03',
              'wowdonate-m18-52', 'gfanqiang010-3', 'wowdonate-sd-21', 'wowdonate-sx-17', 'wowdonate-sd-05',
              'gfanqiang018-2', 'goagent-go-3', 'xinxijishuwyq24', 'wowdonate-m18-24', 'wowdonate-m18-49',
              'gfanqiang014-3', 'gfanqiang006-1', 'wowdonate-m18-51', 'gfanqiang032-0', 'stepfordcuckoos01',
              'wowdonate-sx-21', 'gfanqiang005-8', 'lovejiani040', 'wowdonate-ch-36', 'smartladder017',
              'gfanqiang007-4', 'gfanqiang007-8', 'lovejiani063', 'wowdonate-fuyong-08', 'chromesaiko',
              'smartladder6', 'gfanqiang011-1', 'window8saiko', 'wowdonate-sd-13', 'publicfqe', 'gfanqiang023-2',
              'youxiang01235', 'wowdonate-ch-63', 'xinxijishuwyq23', 'gfanqiang017-3', 'wowdonate-ch-71',
              'wowdonate-lvp-01', 'goagent-go-1', 'gonggongid18', 'gfanqiang005-5', 'overwall3444', 'icezero0010',
              'gfanqiang027-8', 'wowdonate-sd-32', 'starballl6', 'jk9084s-06', 'gfanqiang031-7', 'wowdonate-sx-06',
              'gfanqiang011-5', 'gonggongid03', 'gfanqiang035-8', 'gfanqiang001-1', 'gfanqiang010-7',
              'goagent-public-1', 'youxiang190006', 'gfanqiang014-9', 'wowdonate-m18-03', 'wowdonate-sd-33',
              'jpj4register4', 'wowdonate-sx-15', 'gfanqiang030-8', 'gfanqiang011-6', 'wowdonate-sd-26',
              'publicfqf', 'clannadvbtcozzh', 'cielo032019', 'gfanqiang015-8', 'fgaupdate007', 'gfanqiang009-6',
              'wowdonate-sd-14', 'wowdonate-ch-12', 'gonggongid06', 'wowdonate-sd-38', 'gfanqiang031-6',
              'fghdis9105z-07', 'gfanqiang017-9', 'scnufuyong', 'wowdonate-tts-22', 'wowdonate-ch-32',
              'gfanqiang018-8', 'gfanqiang010-5', 'wowdonate-ch-19', 'gfanqiang000-0', 'gonggongid20',
              'gfanqiang023-3', 'gfanqiang018-6', 'gfanqiang021-0', 'gfanqiang036-8', 'publicfqj', 'smartladder003',
              'wowdonate-sx-02', 'fgaupdate009', 'wowdonate-ch-64', 'baiduchrometieba', 'fgaupdate002', 'lovejiani014',
              'wowdonate-ch-48', 'jpj4register1', 'chromeqq', 'gfanqiang029-5', 'gfanqiang010-0', 'wowdonate-ch-50',
              'gfanqiang015-5', 'gfanqiang026-3', 'gfanqiang020-8', 'wowdonate-m18-34', 'gfanqiang007-0',
              'wowdonate-ch-13', 'smartladder015', 'gfanqiang015-9', 's-g9085h-10', 'x9088-sz-01', 'gfanqiang003-7',
              'wowdonate-fuyong-05', 'wowdonate-ch-80', 'fghdis9105z-08', 'wowdonate-sofa-05', 'xideassassin6',
              'wowdonate-sd-01', 'gfanqiang006-9', 'sfuyong7', 'gfanqiang020-5', 'overwallmax', 'wowdonate-m18-64',
              'wowdonate-ch-10', 'wowdonate-tts-02', 'vi88com8', 'gfanqiang005-0', 'wowdonate-m18-45', 'lovejiani018',
              'gfanqiang013-7', 'gfanqiang011-2', 'gfanqiang036-4', 'smartladder8', 'gfanqiang026-4', 'gfanqiang033-2',
              'gfanqiang004-8', 'lovejiani041', 'wowdonate-sd-28', 'gonggongid09', 'wowdonate-ceec-04',
              'wowdonate-m18-10', 'gonggongid07', 'fgaupdate008', 'wowdonate-ch-34', 'xideassassin2', 'gfanqiang004-0',
              'gfanqiang028-7', 'gfanqiang014-8', 'gfanqiang033-6', 'gfanqiang001-2', 'fgaupdate005', 'gfanqiang004-6',
              'gfanqiang031-5', 'ttphc5', 'gfanqiang031-9', 'gfanqiang002-3', 'gonggongid01', 'wowdonate-m18-17',
              'wowdonate-sofa-16', 'wowdonate-ch-08', 'pdslzq6', 'gfanqiang028-9', 'smartladder019', 'wowdonate-sd-40',
              'kawaiiushionoserve', 'publicfqc', 'wowdonate-ch-16', 'wowdonate-ch-73', 'gfanqiang016-7',
              'gfanqiang026-5', 'wowdonate-ch-43', 'youxiang190003', 'agfw-server-3', 'cielo032022', 'lovejiani043',
              'wowdonate-sd-31', 'gfanqiang022-5', 'wowdonate-ch-52', 'overwallstable', 'vbtcozzhclannad',
              'wowdonate-edg-09', 'wowdonate-edg-03', 'qq362569870', 'moya21928', 'gonggongid11', 'stepfordcuckoos08',
              'gfanqiang012-6', 'gfanqiang019-4', 'wowdonate-m18-36', 'ippotsukobeta', 'gfw46793', 'gfanqiang003-1',
              'cielo032025', 'gfw46794', 'szdsdf9104-01', 'xideassassin4', 'wowdonate-tts-11', 'wowdonate-ch-31',
              'gfanqiang026-0', 'gonggongid08', 'wowdonate-ceec-05', 'overwallgo', 'overwalls1', 'icezero0007',
              'youxiang19000', 'gfanqiang019-6', 'publicgoagent6', 'wowdonate-lvp-11', 'gfanqiang027-0',
              'smartladderus', 'wowdonate-m18-42', 'gfanqiang012-8', 'wowdonate-tts-14', 'wowdonate-m18-13',
              'fgabootstrap004', 'moyc21928', 'wowdonate-m18-23', 'youxiang01238', 'gfanqiang016-8', 'gfanqiang020-4',
              'gfanqiang011-4', 'gfanqiang036-6', 'smartladderchina', 'wowdonate-m18-41', 'wowdonate-sofa-11',
              'wowdonate-ch-68', 'wowdonate-m18-69', 'gfanqiang021-6', 'wowdonate-ch-39', 'gfanqiang021-1', 'saosaiko',
              'wowdonate-ch-15', 'wowdonate-ch-57', 'gfw46796', 'wowdonate-sofa-22', 'gfanqiang023-1', 'gfanqiang010-2',
              'intoskip6', 'icezero0008', 'stepfordcuckoos03', 'agfw-server-1', 'wowdonate-tts-12', 'gfanqiang031-2',
              'gfanqiang018-5', 'overwallchrome', 'overwalltwo', 'gfanqiang025-6', 'wowdonate-ch-51', 'gfanqiang015-0',
              'fghdis9105z-03', 'wowdonate-m18-31', 'szdsdf9104-03', 'x9088-sz-03', 'smartladder013', 'wowdonate-ch-53',
              'icezero0001', 'szdsdf9104-06', 'gfanqiang017-0', 'gfanqiang034-0', 'gfanqiang018-7', 'agfw-server-2',
              'gfanqiang025-4', 'gfanqiang000-6', 'wowdonate-tts-05', 'gfanqiang014-5', 'gfanqiang019-3',
              'akb48daisukilove', 'wowdonate-sd-25', 'wowdonate-100-02', 'wowdonate-100-04', 'lovejiani025',
              'fgabootstrap006', 'gfanqiang022-1', 'youxiang01237', 'publicwallproxy3', 's-g9085h-07', 'ttphc10',
              'gfanqiang030-3', 'wowdonate-tts-24', 'wowdonate-m18-19', 'gfanqiang015-1', 'gfanqiang033-7',
              'wowdonate-lvp-15', 'gfanqiang014-0', 'x9088-sz-08', 'gfanqiang019-2', 'zzhvbtco', 'gonggongid04',
              'gfanqiang029-3', 'wowdonate-badboy-09', 'wowdonate-m18-30', 'gfanqiang027-7', 'gfanqiang030-6',
              'gfanqiang007-9', 'laiguo123', 'wowdonate-ch-06', 'icezero0004', 'clannadzzhvbtco', 'wowdonate-m18-68',
              'cielo032026', 'jk9084s-10', 'wowdonate-m18-47', 'overwallproz', 's-g9085h-08', 'gfanqiang012-7',
              'cielo032021', 'pdslzq5', 'wowdonate-ceec-02', 'gfanqiang004-4', '1.wyq476137265', 'smartladder009',
              'gfanqiang023-6', 'f360uck', 'kawaiiushio4', 'gfanqiang008-2', 'intoskip9', 'fgabootstrap003',
              'fgaupdate006', 'wowdonate-m18-35', 'wowdonate-m18-55', 'wowdonate-edg-10', 'overwallpt',
              'gfanqiang004-5', 'lovejiani061', 'lovejiani012', 'fgaupdate001', 'lovejiani016', 'gfanqiang010-1',
              'jianiwoxiangni', 'wowdonate-lvp-09', 'wowdonate-lvp-13', 'wowdonate-m18-66', 'gfanqiang006-6',
              'gonggongid15', 'overwallcnzz', 'gfanqiang018-3', 'gfanqiang002-5', 'wowdonate-edg-08', 'fghdis9105z-02',
              'gonggongid12', 'overwallfour', 'gfanqiang017-7', 'gfanqiang030-7', 'gfanqiang013-3', 'smartladder4',
              'lovejiani013', 'wowdonate-ch-70', 'fgabootstrap008', 'gfanqiang032-5', 'wowdonate-m18-54',
              'gfanqiang031-4', 'gonggongid10', 'wowdonate-ch-17', 'wowdonate-m18-21', 'gfanqiang016-6', 'jk9084s-04',
              'smartladderuk', 'wowdonate-ch-54', 'gfanqiang011-3', 'gfanqiang004-2', 'starballl9', 'intoskip8',
              'wowdonate-sofa-18', 'lovejiani045', 'ttphc8', 'gfanqiang018-0', 'wowdonate-sx-18', 'lovejiani020',
              'gfanqiang023-0', 'lovejiani021', 'lovejiani011', 'lovejiani017', 'gfanqiang030-9', 'gfanqiang028-4',
              'jk9084s-09', 'wowdonate-ch-05', 'gfanqiang021-5', 'gfanqiang003-5', 'gfanqiang005-2', 'gfanqiang024-9',
              'wowdonate-fuyong-09', 'gfanqiang030-4', 'gfanqiang021-4', 'wowdonate-tts-27', 'wowdonate-ch-77',
              'gfanqiang005-3', 'x9088-sz-10', 'gonggongid19', 'wowdonate-sd-24', 'lovejiani008', 'wowdonate-sx-11',
              'wowdonate-fuyong-07', 'wowdonate-tts-19', 'smartladder008', 'kawaiiushio6', 'wowdonate-m18-16',
              'youxiang190009', 'pdslzq8', 'publicfqg', 'wowdonate-lvp-08', 'gfanqiang001-8', 'gfanqiang029-9',
              'lovejiani005', 'xideassassin8', 'gfanqiang002-4', 'youxiang190004', 'gfanqiang020-1', 'gfanqiang034-5',
              '1.wangyuqi19961213', 'fganr001', 'nbslf7', 'moyb21928', 'gfanqiang021-8', 'nbslf11', 'gfanqiang032-7',
              'gfanqiang036-2', 'stepfordcuckoos06', 'fganr002', 'overwallpro', 'gfanqiang013-2', 'icezero0002',
              'wowdonate-ch-74', 'gfanqiang027-6', 'fghdis9105z-06', 'gonggongid14', 'vbtcoclannadzzh',
              'wowdonate-badboy-06', 'gfanqiang022-9', 'kawaiiushio3', 'smartladder3', 'gfanqiang000-2',
              'wowdonate-ch-04', 'smartladder010', 'gfanqiang007-2', 'wowdonate-tts-13', 'publicwallproxy2',
              'gfanqiang032-8', 'wowdonate-m18-43', 'szdsdf9104-05', 'baidufirefoxtieba', 'gfanqiang027-3',
              'gfw46797', 'wowdonate-ch-38', 'gfanqiang003-9', 'gfanqiang029-7', 'wowdonate-tts-06', 'gfanqiang000-1',
              'fgabootstrap007', 'overwallorz', 'wowdonate-ch-44', 'gfanqiang011-7', 'smartladder006', 'smartladder1',
              'gfanqiang015-2', 'wowdonate-m18-07', 'lovejiani022', 'jk9084s-08', 'lovejiani039', 'wowdonate-ch-24',
              'gfanqiang012-5', 'wowdonate-lvp-14', 'gfanqiang004-3', 'wowdonate-m18-63', 'xideassassin7',
              'gfanqiang024-8', 'gfanqiang011-9', 'gfanqiang031-3', 'gfanqiang002-0', 'zzhvbtcoclannad',
              'wowdonate-sx-12', 'jk9084s-05', 'wowdonate-hj-04', 'publicgoagent3', 'gfanqiang013-8', 's-g9085h-06',
              'gfanqiang019-5', 'overwallbeta', 'lovejiani066', 'wowdonate-ch-60', 'gfanqiang021-7', 'wowdonate-ch-30',
              'publicfqh', 'x9088-sz-02', 'vi88com3', 'wowdonate-tts-26', 'wowdonate-ch-42', 'wowdonate-badboy-02',
              'wowdonate-tts-18', 'gfanqiang000-7', 'gfanqiang013-4', 'wowdonate-m18-44', 'gfanqiang022-6',
              'wowdonate-m18-33', 'lovejiani010', 'gfanqiang001-9', 'lovejiani067', 'lovejiani071', 'flowerwakawaii',
              'wowdonate-m18-59', 's-g9085h-03', 'gfanqiang017-1', 'gfanqiang016-9', 'wowdonate-lvp-12',
              'overwallchina', 'wowdonate-ch-27', 'wowdonate-sofa-21', 'gfanqiang003-4', 'szdsdf9104-07',
              'wowdonate-hj-05', 'gfanqiang025-0', 'wowdonate-edg-02', 'gonggongid13', 'youxiang190001',
              'wowdonate-ch-11', 'wowdonate-m18-01', 'gfanqiang028-6', 'lovejiani0641', 'wowdonate-ch-33',
              'gfanqiang009-4', 'wowdonate-lvp-07', 'gfanqiang006-4', 'wowdonate-sofa-01', 'stepfordcuckoos09',
              'wowdonate-ch-45', 'goagent-public-2', 'gfanqiang008-9', 'gfanqiang025-5', 'gfanqiang034-2',
              'wowdonate-m18-12', 'gfanqiang005-9', 'wowdonate-m18-20', 'wowdonate-100-00', 'fghdis9105z-01',
              'wowdonate-ch-37', 'lovejiani046', 'youxiang01232', 'gfanqiang008-4', 'gfanqiang011-8',
              'wowdonate-sd-37', 'gfanqiang034-4', 'youxiang01231', 'wowdonate-sx-16', 'gfanqiang012-2',
              'smartladder012', 'intoskip5', 'gfanqiang019-9', 'wowdonate-sofa-12', 'gfanqiang034-8', 'wowdonate-sx-20',
              'wowdonate-sx-00', 'wowdonate-m18-11', 'xideassassin3', 'gfanqiang028-8', 'wowdonate-m18-18',
              'wowdonate-m18-08', 'wowdonate-sx-14', 'gfanqiang000-8', 'gfanqiang019-7', 'wowdonate-tts-15',
              'smartladder002', 'wowdonate-sofa-14', 'lovejiani048', 'stepfordcuckoos07', 'wowdonate-ch-07',
              'gfanqiang018-9', 'vi88com4', 'goagent-go-4', 'wowdonate-sd-10', 'wowdonate-hj-08', 'gfanqiang035-9',
              'lovejiani009', 'wowdonate-ch-56', 'gfanqiang027-5', 'wowdonate-ceec-06', 'gfanqiang007-7',
              'gfanqiang019-8', 'gfanqiang029-4', 'fghdis9105z-09', 'nbslf6', 's-g9085h-09', 'lovejiani069',
              'gfanqiang023-5', 'wowdonate-edg-04', 'lovejiani055', 'publicwallproxy4', 'lovejiani029',
              'gfanqiang032-1', 'gfanqiang014-6', 'gfanqiang008-7', 'x9088-sz-07', 'gfanqiang033-4', 'fgabootstrap005',
              'wowdonate-tts-10', 'gfanqiang015-4', 'gfanqiang028-3', 'gfanqiang035-3', 'gfanqiang014-2',
              'gfanqiang035-6', 'gfanqiang018-4', 'gfanqiang024-7', 'gfanqiang034-3', 'wowdonate-fuyong-03',
              'gfanqiang012-3', 'wowdonate-m18-61', 'cielo032024', 'publicgoagent8', 'lovejiani057', 'lovejiani007',
              'gfanqiang025-1', 'lovejiani033', 'gfanqiang035-2', 'icezero0005', 'wowdonate-100-06', 'gfanqiang008-1',
              'wowdonate-sd-36', 'lovejiani037', 'wowdonate-ch-76', 'wowdonate-lvp-18', 'gfanqiang016-3',
              'gfanqiang016-5', 'gfanqiang009-1', 'gfanqiang026-2', 'gfanqiang024-0', 'wowdonate-hj-06', 'clannadvbtco',
              'gfanqiang006-2', 'gfanqiang016-0', 'gfanqiang023-9', 'wowdonate-hj-09', 'gfanqiang035-5', 'sfuyong6',
              'wowdonate-fuyong-02', 'wowdonate-tts-09', 'gfanqiang012-4', 'gfanqiang009-5', 'wowdonate-m18-02',
              'gfanqiang021-9', 'gfanqiang022-7', 'youxiang012310', 'lovejiani047', 'szdsdf9104-09', 'wowdonate-sx-13',
              'wowdonate-100-07', 'gfanqiang030-1', 'gfanqiang001-6', 'gfanqiang017-6', 'lovejiani049',
              'wowdonate-sd-39']

# txwgtxwg-01|txwgtxwg-02|txwgtxwg-03|txwgtxwg-04|txwgtxwg-05|txwgtxwg-06|txwgtxwg-07|txwgtxwg-08|txwgtxwg-09|txwgtxwg-10|txwg001|txwg002|txwg003|txwg004|txwg005|txwg006|txwg007|txwg008|txwg009|txwg010|jzz1945-01|jzz1945-02|jzz1945-03|jzz1945-04|jzz1945-05|jzz1945-006|jzz1945-007|jzz1945-008|jzz1945-009|jzz1945-010|iwatch-215-01|jzz1946-01|jzz1946-02|jzz1946-03|jzz1946-04|jzz1946-05|jzz1946-06|jzz1946-07|jzz1946-08|jzz1946-09|jzz1946-10|jzz1947-01|jzz1947-02|jzz1947-03|jzz1947-04|jzz1947-05|jzz1947-06|jzz1947-07|jzz1947-08|jzz1947-09|jzz1947-10|jzz1948-01|jzz1948-02|jzz1948-03|jzz1948-04|jzz1948-05|jzz1948-06|jzz1948-07|jzz1948-08|jzz1948-09|jzz1948-10|jzzjzz00101|jzzjzz00102|jzzjzz00103|jzzjzz01|jzzjzz002|jzzjzz003|jzzjzz004|jzzjzz005|jzzjzz006|jzzjzz001|txwgtxwg001-01|txwgtxwg001-02|txwgtxwg001-03|txwgtxwg001-04|txwgtxwg001-05|txwgtxwg001-06|txwgtxwg001-07|txwgtxwg001-08|txwgtxwg001-09|txwgtxwg001-10|txwgtxwg002-01|txwgtxwg002-02|txwgtxwg002-03|txwgtxwg002-04|txwgtxwg002-05|txwgtxwg002-06|txwgtxwg002-07|txwgtxwg002-08|txwgtxwg002-09|txwgtxwg002-10|txwgtxwg003-01|txwgtxwg003-02|txwgtxwg003-03|txwgtxwg003-04|txwgtxwg003-05|txwgtxwg003-06|txwgtxwg003-07|txwgtxwg003-08|txwgtxwg003-09|txwgtxwg003-10|txwgtxwg004-01|txwgtxwg004-02|txwgtxwg004-03|txwgtxwg004-04|txwgtxwg004-05|txwgtxwg004-06|txwgtxwg004-07|txwgtxwg004-08|txwgtxwg004-09|txwgtxwg004-10|txwgtxwg005-01|txwgtxwg005-02|txwgtxwg005-03|txwgtxwg005-04|txwgtxwg005-05|txwgtxwg005-06|txwgtxwg005-07|txwgtxwg005-08|txwgtxwg005-09|txwgtxwg005-10|txwgtxwg006-01|txwgtxwg006-02|txwgtxwg006-03|txwgtxwg006-04|txwgtxwg006-05|txwgtxwg006-06|txwgtxwg006-07|txwgtxwg006-08|txwgtxwg006-09|txwgtxwg006-10|txwgtxwg007-01|txwgtxwg007-02|txwgtxwg007-03|txwgtxwg007-04|txwgtxwg007-05|txwgtxwg007-06|txwgtxwg007-07|txwgtxwg007-08|txwgtxwg007-09|txwgtxwg007-10|txwgtxwg008-01|txwgtxwg008-02|txwgtxwg008-03|txwgtxwg008-04|txwgtxwg008-05|txwgtxwg008-06|txwgtxwg008-07|txwgtxwg008-08|txwgtxwg008-09|txwgtxwg008-10|txwgtxwg009-01|txwgtxwg009-02|txwgtxwg009-03|txwgtxwg009-04|txwgtxwg009-05|txwgtxwg009-06|txwgtxwg009-07|txwgtxwg009-08|txwgtxwg009-09|txwgtxwg009-10|txwgtxwg010-01|txwgtxwg010-02|txwgtxwg010-03|txwgtxwg010-04|txwgtxwg010-05|txwgtxwg010-06|txwgtxwg010-07|txwgtxwg010-08|txwgtxwg010-09|txwgtxwg010-10|txwgtxwg011-01|txwgtxwg011-02|txwgtxwg011-03|txwgtxwg011-04|txwgtxwg011-05|txwgtxwg011-06|txwgtxwg011-07|txwgtxwg011-08|txwgtxwg011-09|txwgtxwg011-10|txwgtxwg012-01|txwgtxwg012-02|txwgtxwg012-03|txwgtxwg012-04|txwgtxwg012-05|txwgtxwg012-06|txwgtxwg012-07|txwgtxwg012-08|txwgtxwg012-09|txwgtxwg012-10|txwgtxwg013-01|txwgtxwg013-02|txwgtxwg013-03|txwgtxwg013-04|txwgtxwg013-05|txwgtxwg013-06|txwgtxwg013-07|txwgtxwg013-08|txwgtxwg013-09|txwgtxwg013-010|txwgtxwg014-01|txwgtxwg014-02|txwgtxwg014-03|txwgtxwg014-04|txwgtxwg014-05|txwgtxwg014-06|txwgtxwg014-07|txwgtxwg014-08|txwgtxwg014-09|txwgtxwg014-10|txwgtxwg015-01|txwgtxwg015-02|txwgtxwg015-03|txwgtxwg015-04|txwgtxwg015-05|txwgtxwg015-06|txwgtxwg015-07|txwgtxwg015-08|txwgtxwg015-09|txwgtxwg015-10|txwgtxwg016-01|txwgtxwg016-02|txwgtxwg016-03|txwgtxwg016-04|txwgtxwg016-05|txwgtxwg016-06|txwgtxwg016-07|txwgtxwg016-08|txwgtxwg016-09|txwgtxwg016-10|txwgtxwg017-01|txwgtxwg017-02|txwgtxwg017-03|txwgtxwg017-04|txwgtxwg017-05|txwgtxwg017-06|txwgtxwg017-07|txwgtxwg017-08|txwgtxwg017-09|txwgtxwg017-10|txwgtxwg018-01|txwgtxwg018-02|txwgtxwg018-03|txwgtxwg018-04|txwgtxwg018-05|txwgtxwg018-06|txwgtxwg018-07|txwgtxwg018-08|txwgtxwg018-09|txwgtxwg018-10|txwgtxwg019-01|txwgtxwg019-02|txwgtxwg019-03|txwgtxwg019-04|txwgtxwg019-05|txwgtxwg019-06|txwgtxwg019-07|txwgtxwg019-08|txwgtxwg019-09|txwgtxwg019-10|txwgtxwg020-01|txwgtxwg020-02|txwgtxwg020-03|txwgtxwg020-04|txwgtxwg020-05|txwgtxwg020-06|txwgtxwg020-07|txwgtxwg020-08|txwgtxwg020-09|txwgtxwg020-10|txwgtxwg021-01|txwgtxwg021-02|txwgtxwg021-03|txwgtxwg021-04|txwgtxwg021-05|txwgtxwg021-06|txwgtxwg021-07|txwgtxwg021-08|txwgtxwg021-09|txwgtxwg021-10|txwgtxwg022-01|txwgtxwg022-02|txwgtxwg022-03|txwgtxwg022-04|txwgtxwg022-05|txwgtxwg022-06|txwgtxwg022-07|txwgtxwg022-08|txwgtxwg022-09|txwgtxwg022-10|txwgtxwg023-01|txwgtxwg023-02|txwgtxwg023-03|txwgtxwg023-04|txwgtxwg023-05|txwgtxwg023-06|txwgtxwg023-07|txwgtxwg023-08|txwgtxwg023-09|txwgtxwg023-10|txwgtxwg024-01|txwgtxwg024-02|txwgtxwg024-03|txwgtxwg024-04|txwgtxwg024-05|txwgtxwg024-06|txwgtxwg024-07|txwgtxwg024-08|txwgtxwg024-09|txwgtxwg024-10|txwgtxwg025-01|txwgtxwg025-02|txwgtxwg025-03|txwgtxwg025-04|txwgtxwg025-05|txwgtxwg025-06|txwgtxwg025-07|txwgtxwg025-08|txwgtxwg025-09|txwgtxwg025-10|txwgtxwg026-01|txwgtxwg026-02|txwgtxwg026-03|txwgtxwg026-04|txwgtxwg026-05|txwgtxwg026-06|txwgtxwg026-07|txwgtxwg026-08|txwgtxwg026-09|txwgtxwg026-10|txwgtxwg027-01|txwgtxwg027-02|txwgtxwg027-03|txwgtxwg027-04|txwgtxwg027-05|txwgtxwg027-06|txwgtxwg027-07|txwgtxwg027-08|txwgtxwg027-09|txwgtxwg027-10|txwgtxwg028-01|txwgtxwg028-02|txwgtxwg028-03|txwgtxwg028-04|txwgtxwg028-05|txwgtxwg028-06|txwgtxwg028-07|txwgtxwg028-08|txwgtxwg028-09|txwgtxwg028-10|txwgtxwg029-01|txwgtxwg029-02|txwgtxwg029-03|txwgtxwg029-04|txwgtxwg029-05|txwgtxwg029-06|txwgtxwg029-07|txwgtxwg029-08|txwgtxwg029-09|txwgtxwg029-10|txwgtxwg030-01|txwgtxwg030-02|txwgtxwg030-03|txwgtxwg030-04|txwgtxwg030-05|txwgtxwg030-06|txwgtxwg030-07|txwgtxwg030-08|txwgtxwg030-09|txwgtxwg030-10|txwgtxwg031-01|txwgtxwg031-02|txwgtxwg031-03|txwgtxwg031-04|txwgtxwg031-05|txwgtxwg031-06|txwgtxwg031-07|txwgtxwg031-08|txwgtxwg031-09|txwgtxwg031-10|txwgtxwg032-01|txwgtxwg032-02|txwgtxwg032-03|txwgtxwg032-04|txwgtxwg032-05|txwgtxwg032-06|txwgtxwg032-07|txwgtxwg032-08|txwgtxwg032-09|txwgtxwg032-10|txwgtxwg033-01|txwgtxwg033-02|txwgtxwg033-03|txwgtxwg033-04|txwgtxwg033-05|txwgtxwg033-06|txwgtxwg033-07|txwgtxwg033-08|txwgtxwg033-09|txwgtxwg033-10|txwgtxwg034-01|txwgtxwg034-02|txwgtxwg034-03|txwgtxwg034-04|txwgtxwg034-05|txwgtxwg034-06|txwgtxwg034-07|txwgtxwg034-08|txwgtxwg034-09|txwgtxwg034-10|txwgtxwg035-01|txwgtxwg035-02|txwgtxwg035-03|txwgtxwg035-04|txwgtxwg035-05|txwgtxwg035-06|txwgtxwg035-07|txwgtxwg035-08|txwgtxwg035-09|txwgtxwg035-10|txwgtxwg036-01|txwgtxwg036-02|txwgtxwg036-03|txwgtxwg036-04|txwgtxwg036-05|txwgtxwg036-06|txwgtxwg036-07|txwgtxwg036-08|txwgtxwg036-09|txwgtxwg036-10|txwgtxwg037-01|txwgtxwg037-02|txwgtxwg037-03|txwgtxwg037-04|txwgtxwg037-05|txwgtxwg037-06|txwgtxwg037-07|txwgtxwg037-08|txwgtxwg037-09|txwgtxwg037-10|txwgtxwg038-01|txwgtxwg038-02|txwgtxwg038-03|txwgtxwg038-04|txwgtxwg038-05|txwgtxwg038-06|txwgtxwg038-07|txwgtxwg038-08|txwgtxwg038-09|txwgtxwg038-10|txwgtxwg039-01|txwgtxwg039-02|txwgtxwg039-03|txwgtxwg039-04|txwgtxwg039-05|txwgtxwg039-06|txwgtxwg039-07|txwgtxwg039-08|txwgtxwg039-09|txwgtxwg039-10|txwgtxwg040-01|txwgtxwg040-02|txwgtxwg040-03|txwgtxwg040-04|txwgtxwg040-05|txwgtxwg040-06|txwgtxwg040-07|txwgtxwg040-08|txwgtxwg040-09|txwgtxwg040-10|txwgtxwg041-01|txwgtxwg041-02|txwgtxwg041-03|txwgtxwg041-04|txwgtxwg041-05|txwgtxwg041-06|txwgtxwg041-07|txwgtxwg041-08|txwgtxwg041-09|txwgtxwg041-10|txwgtxwg042-01|txwgtxwg042-02|txwgtxwg042-03|txwgtxwg042-04|txwgtxwg042-05|txwgtxwg042-06|txwgtxwg042-07|txwgtxwg042-08|txwgtxwg042-09|txwgtxwg042-10|txwgtxwg043-01|txwgtxwg043-02|txwgtxwg043-03|txwgtxwg043-04|txwgtxwg043-05|txwgtxwg043-06|txwgtxwg043-07|txwgtxwg043-08|txwgtxwg043-09|txwgtxwg043-10|txwgtxwg044-01|txwgtxwg044-02|txwgtxwg044-03|txwgtxwg044-04|txwgtxwg044-05|txwgtxwg044-06|txwgtxwg044-07|txwgtxwg044-08|txwgtxwg044-09|txwgtxwg044-10|txwgtxwg045-01|txwgtxwg045-02|txwgtxwg045-03|txwgtxwg045-04|txwgtxwg045-05|txwgtxwg045-06|txwgtxwg045-07|txwgtxwg045-08|txwgtxwg045-09|txwgtxwg045-10|txwgtxwg046-01|txwgtxwg046-02|txwgtxwg046-03|txwgtxwg046-04|txwgtxwg046-05|txwgtxwg046-06|txwgtxwg046-07|txwgtxwg046-08|txwgtxwg046-09|txwgtxwg046-10|txwgtxwg047-01|txwgtxwg047-02|txwgtxwg047-03|txwgtxwg047-04|txwgtxwg047-05|txwgtxwg047-06|txwgtxwg047-07|txwgtxwg047-08|txwgtxwg047-09|txwgtxwg047-10|txwgtxwg048-01|txwgtxwg048-02|txwgtxwg048-03|txwgtxwg048-04|txwgtxwg048-05|txwgtxwg048-06|txwgtxwg048-07|txwgtxwg048-08|txwgtxwg048-09|txwgtxwg048-10|txwgtxwg049-01|txwgtxwg049-02|txwgtxwg049-03|txwgtxwg049-04|txwgtxwg049-05|txwgtxwg049-06|txwgtxwg049-07|txwgtxwg049-08|txwgtxwg049-09|txwgtxwg049-10|txwgtxwg050-01|txwgtxwg050-02|txwgtxwg050-03|txwgtxwg050-04|txwgtxwg050-05|txwgtxwg050-06|txwgtxwg050-07|txwgtxwg050-08|txwgtxwg050-09|txwgtxwg050-10|txwgtxwg051-01|txwgtxwg051-02|txwgtxwg051-03|txwgtxwg051-04|txwgtxwg051-05|txwgtxwg051-06|txwgtxwg051-07|txwgtxwg051-08|txwgtxwg051-09|txwgtxwg051-10|txwgtxwg052-01|txwgtxwg052-02|txwgtxwg052-03|txwgtxwg052-04|txwgtxwg052-05|txwgtxwg052-06|txwgtxwg052-07|txwgtxwg052-08|txwgtxwg052-09|txwgtxwg052-10|txwgtxwg053-01|txwgtxwg053-02|txwgtxwg053-03|txwgtxwg053-04|txwgtxwg053-05|txwgtxwg053-06|txwgtxwg053-07|txwgtxwg053-08|txwgtxwg053-09|txwgtxwg053-10|txwgtxwg054-01|txwgtxwg054-02|txwgtxwg054-03|txwgtxwg054-04|txwgtxwg054-05|txwgtxwg054-06|txwgtxwg054-07|txwgtxwg054-08|txwgtxwg054-09|txwgtxwg054-10|txwgtxwg055-01|txwgtxwg055-02|txwgtxwg055-03|txwgtxwg055-04|txwgtxwg055-05|txwgtxwg055-06|txwgtxwg055-07|txwgtxwg055-08|txwgtxwg055-09|txwgtxwg055-10|txwgtxwg056-01|txwgtxwg056-02|txwgtxwg056-03|txwgtxwg056-04|txwgtxwg056-05|txwgtxwg056-06|txwgtxwg056-07|txwgtxwg056-08|txwgtxwg056-09|txwgtxwg056-10|txwgtxwg057-01|txwgtxwg057-02|txwgtxwg057-03|txwgtxwg057-04|txwgtxwg057-05|txwgtxwg057-06|txwgtxwg057-07|txwgtxwg057-08|txwgtxwg057-09|txwgtxwg057-10|txwgtxwg058-01|txwgtxwg058-02|txwgtxwg058-03|txwgtxwg058-04|txwgtxwg058-05|txwgtxwg058-06|txwgtxwg058-07|txwgtxwg058-08|txwgtxwg058-09|txwgtxwg058-10|txwgtxwg059-01|txwgtxwg059-02|txwgtxwg059-03|txwgtxwg059-04|txwgtxwg059-05|txwgtxwg059-06|txwgtxwg059-07|txwgtxwg059-08|txwgtxwg059-09|txwgtxwg059-10|txwgtxwg060-01|txwgtxwg060-02|txwgtxwg060-03|txwgtxwg060-04|txwgtxwg060-05|txwgtxwg060-06|txwgtxwg060-07|txwgtxwg060-08|txwgtxwg060-09|txwgtxwg060-10|txwgtxwg061-01|txwgtxwg061-02|txwgtxwg061-03|txwgtxwg061-04|txwgtxwg061-05|txwgtxwg061-06|txwgtxwg061-07|txwgtxwg061-08|txwgtxwg061-09|txwgtxwg061-10|txwgtxwg062-01|txwgtxwg062-02|txwgtxwg062-03|txwgtxwg062-04|txwgtxwg062-05|txwgtxwg062-06|txwgtxwg062-07|txwgtxwg062-08|txwgtxwg062-09|txwgtxwg062-10|txwgtxwg063-01|txwgtxwg063-02|txwgtxwg063-03|txwgtxwg063-04|txwgtxwg063-05|txwgtxwg063-06|txwgtxwg063-07|txwgtxwg063-08|txwgtxwg063-09|txwgtxwg063-10|txwgtxwg064-01|txwgtxwg064-02|txwgtxwg064-03|txwgtxwg064-04|txwgtxwg064-05|txwgtxwg064-06|txwgtxwg064-07|txwgtxwg064-08|txwgtxwg064-09|txwgtxwg064-10|txwgtxwg65-01|txwgtxwg65-02|txwgtxwg65-03|txwgtxwg65-04|txwgtxwg65-05|txwgtxwg65-06|txwgtxwg65-07|txwgtxwg65-08|txwgtxwg65-09|txwgtxwg65-10|txwgtxwg0066-01|txwgtxwg0066-02|txwgtxwg0066-03|txwgtxwg0066-04|txwgtxwg0066-05|txwgtxwg0066-06|txwgtxwg0066-07|txwgtxwg0066-08|txwgtxwg0066-09|txwgtxwg0066-10|txwgtxwg67-01|txwgtxwg67-02|txwgtxwg67-03|txwgtxwg67-04|txwgtxwg67-05|txwgtxwg67-06|txwgtxwg67-07|txwgtxwg67-08|txwgtxwg67-09|txwgtxwg67-10|txwgtxwg68-01|txwgtxwg68-02|txwgtxwg68-03|txwgtxwg68-04|txwgtxwg68-05|txwgtxwg68-06|txwgtxwg68-07|txwgtxwg68-08|txwgtxwg68-09|txwgtxwg68-10|txwgtxwg69-01|txwgtxwg69-02|txwgtxwg69-03|txwgtxwg69-04|txwgtxwg69-05|txwgtxwg69-06|txwgtxwg69-07|txwgtxwg69-08|txwgtxwg69-09|txwgtxwg69-10|txwgtxwg70-01|txwgtxwg70-02|txwgtxwg70-03|txwgtxwg70-04|txwgtxwg70-05|txwgtxwg70-06|txwgtxwg70-07|txwgtxwg70-08|txwgtxwg70-09|txwgtxwg70-10|txwgtxwg71-01|txwgtxwg71-02|txwgtxwg71-03|txwgtxwg71-04|txwgtxwg71-05|txwgtxwg71-06|txwgtxwg71-07|txwgtxwg71-08|txwgtxwg71-09|txwgtxwg71-10|txwgtxwg72-01|txwgtxwg72-02|txwgtxwg72-03|txwgtxwg72-04|txwgtxwg72-05|txwgtxwg72-06|txwgtxwg72-07|txwgtxwg72-08|txwgtxwg72-09|txwgtxwg72-10|txwgtxwg73-01|txwgtxwg73-02|txwgtxwg73-03|txwgtxwg73-04|txwgtxwg73-05|txwgtxwg73-06|txwgtxwg73-07|txwgtxwg73-08|txwgtxwg73-09|txwgtxwg73-10|txwgtxwg74-01|txwgtxwg74-02|txwgtxwg74-03|txwgtxwg74-04|txwgtxwg74-05|txwgtxwg74-06|txwgtxwg74-07|txwgtxwg74-08|txwgtxwg74-09|txwgtxwg74-10|txwgtxwg75-01|txwgtxwg75-02|txwgtxwg75-03|txwgtxwg75-04|txwgtxwg75-05|txwgtxwg75-06|txwgtxwg75-07|txwgtxwg75-08|txwgtxwg75-09|txwgtxwg75-10|txwgtxwg76-01|txwgtxwg76-02|txwgtxwg76-03|txwgtxwg76-04|txwgtxwg76-05|txwgtxwg76-06|txwgtxwg76-07|txwgtxwg76-08|txwgtxwg76-09|txwgtxwg76-10|txwgtxwg77-01|txwgtxwg77-02|txwgtxwg77-03|txwgtxwg77-04|txwgtxwg77-05|txwgtxwg77-06|txwgtxwg77-07|txwgtxwg77-08|txwgtxwg77-09|txwgtxwg77-10|txwgtxwg78-01|txwgtxwg78-02|txwgtxwg78-03|txwgtxwg78-04|txwgtxwg78-05|txwgtxwg78-06|txwgtxwg78-07|txwgtxwg78-08|txwgtxwg78-09|txwgtxwg78-10|txwgtxwg79-01|txwgtxwg79-02|txwgtxwg79-03|txwgtxwg79-04|txwgtxwg79-05|txwgtxwg79-06|txwgtxwg79-07|txwgtxwg79-08|txwgtxwg79-09|0txwgtxwg79-10|txwgtxwg80-01|txwgtxwg80-02|txwgtxwg80-03|txwgtxwg80-04|txwgtxwg80-05|txwgtxwg80-06|txwgtxwg80-07|txwgtxwg80-08|txwgtxwg80-09|txwgtxwg80-10|txwgtxwg81-01|txwgtxwg81-02|txwgtxwg81-03|txwgtxwg81-04|txwgtxwg81-05|txwgtxwg81-06|txwgtxwg81-07|txwgtxwg81-08|txwgtxwg81-09|txwgtxwg81-10|txwgtxwg82-01|txwgtxwg82-02|txwgtxwg82-03|txwgtxwg82-04|txwgtxwg82-05|txwgtxwg82-06|txwgtxwg82-07|txwgtxwg82-08|txwgtxwg82-09|txwgtxwg82-10|txwgtxwg83-01|txwgtxwg83-02|txwgtxwg83-03|txwgtxwg83-04|txwgtxwg83-05|txwgtxwg83-06|txwgtxwg83-07|txwgtxwg83-08|txwgtxwg83-09|txwgtxwg83-10|txwgtxwg84-01|txwgtxwg84-02|txwgtxwg84-03|txwgtxwg84-04|txwgtxwg84-05|txwgtxwg84-06|txwgtxwg84-07|txwgtxwg84-08|txwgtxwg84-09|txwgtxwg84-10|txwgtxwg85-01|txwgtxwg85-02|txwgtxwg85-03|txwgtxwg85-04|txwgtxwg85-05|txwgtxwg85-06|txwgtxwg85-07|txwgtxwg85-08|txwgtxwg85-09|txwgtxwg85-10|txwgtxwg86-01|txwgtxwg86-02|txwgtxwg86-03|txwgtxwg86-04|txwgtxwg86-05|txwgtxwg86-06|txwgtxwg86-07|txwgtxwg86-08|txwgtxwg86-09|txwgtxwg86-10|txwgtxwg87-01|txwgtxwg87-02|txwgtxwg87-03|txwgtxwg87-04|txwgtxwg87-05|txwgtxwg87-06|txwgtxwg87-07|txwgtxwg87-08|txwgtxwg87-09|txwgtxwg87-10|txwgtxwg88-01|txwgtxwg88-02|txwgtxwg88-03|txwgtxwg88-04|txwgtxwg88-05|txwgtxwg88-06|txwgtxwg88-07|txwgtxwg88-08|txwgtxwg88-09|txwgtxwg88-10|txwgtxwg89-01|txwgtxwg89-02|txwgtxwg89-03|txwgtxwg89-04|txwgtxwg89-05|txwgtxwg89-06|txwgtxwg89-07|txwgtxwg89-08|txwgtxwg89-09|txwgtxwg89-10|txwgtxwg90-01|txwgtxwg90-02|txwgtxwg90-03|txwgtxwg90-04|txwgtxwg90-05|txwgtxwg90-06|txwgtxwg90-07|txwgtxwg90-08|txwgtxwg90-09|txwgtxwg90-10|txwgtxwg91-01|txwgtxwg91-02|txwgtxwg91-03|txwgtxwg91-04|txwgtxwg91-05|txwgtxwg91-06|txwgtxwg91-07|txwgtxwg91-08|txwgtxwg91-09|txwgtxwg91-10|txwgtxwg92-01|txwgtxwg92-02|txwgtxwg92-03|txwgtxwg92-04|txwgtxwg92-05|txwgtxwg92-06|txwgtxwg92-07|txwgtxwg92-08|txwgtxwg92-09|txwgtxwg92-10|txwgtxwg93-01|txwgtxwg93-02|txwgtxwg93-03|txwgtxwg93-04|txwgtxwg93-05|txwgtxwg93-06|txwgtxwg93-07|txwgtxwg93-08|txwgtxwg93-09|txwgtxwg93-10|txwgtxwg94-01|txwgtxwg94-02|txwgtxwg94-03|txwgtxwg94-04|txwgtxwg94-05|txwgtxwg94-06|txwgtxwg94-07|txwgtxwg94-08|txwgtxwg94-09|txwgtxwg94-10|txwgtxwg95-01|txwgtxwg95-02|txwgtxwg95-03|txwgtxwg95-04|txwgtxwg95-05|txwgtxwg95-06|txwgtxwg95-07|txwgtxwg95-08|txwgtxwg95-09|txwgtxwg95-10|txwgtxwg96-01|txwgtxwg96-02|txwgtxwg96-03|txwgtxwg96-04|txwgtxwg96-05|txwgtxwg96-06|txwgtxwg96-07|txwgtxwg96-08|txwgtxwg96-09|txwgtxwg96-10|txwgtxwg97-01|txwgtxwg97-02|txwgtxwg97-03|txwgtxwg97-04|txwgtxwg97-05|txwgtxwg97-06|txwgtxwg97-07|txwgtxwg97-08|txwgtxwg97-09|txwgtxwg97-10|txwgtxwg98-01|txwgtxwg98-02|txwgtxwg98-03|txwgtxwg98-04|txwgtxwg98-05|txwgtxwg98-06|txwgtxwg98-07|txwgtxwg98-08|txwgtxwg98-09|txwgtxwg98-10|txwgtxwg99-01|txwgtxwg99-02|txwgtxwg99-03|txwgtxwg99-04|txwgtxwg99-05|txwgtxwg99-06|txwgtxwg99-07|txwgtxwg99-08|txwgtxwg99-09|txwgtxwg99-10|txwgtxwg0100-01|txwgtxwg0100-02|txwgtxwg0100-03|txwgtxwg0100-04|txwgtxwg0100-05|txwgtxwg0100-06|txwgtxwg0100-07|txwgtxwg0100-08|txwgtxwg0100-09|txwgtxwg0100-10|txwgtxwg0101-01|txwgtxwg0101-02|txwgtxwg

for i in range(1, 60):
    T2_APP_IDS.append('goagentbiz%s' % i)

if len(sys.argv) > 1:
    T1_APP_IDS = [sys.argv[1]]
    T2_APP_IDS = []

random.shuffle(T1_APP_IDS)
random.shuffle(T2_APP_IDS)

APP_ID_QUEUE = gevent.queue.Queue(items=T1_APP_IDS + T2_APP_IDS)

class FakeClient(object):
    def __init__(self):
        self.host = ''
        self.dst_ip = ''

    def add_resource(self, res):
        pass


class FakeProxy(object):
    def __init__(self, fetch_server):
        self.fetch_server = fetch_server


good_app_ids = set()

def check():
    while True:
        if len(good_app_ids) >= 40:
            return
        appid = APP_ID_QUEUE.get()
        try:
            app_status = fqsocks.proxies.goagent.gae_urlfetch(
                FakeClient(), FakeProxy('https://%s.appspot.com/2?' % appid),
                'GET', 'http://www.baidu.com', {}, '').app_status
            LOGGER.info('%s => %s\n' % (appid, app_status))
            if app_status == 200:
                if len(good_app_ids) >= 40:
                    return
                good_app_ids.add(appid)
        except:
            traceback.print_exc()

def main():
    gevent.monkey.patch_all()
    fqsocks.proxies.goagent.GoAgentProxy.GOOGLE_IPS = socket.gethostbyname_ex('goagent-google-ip.fqrouter.com')[2]
    while True:
        greenlets = []
        for i in range(8):
            greenlets.append(gevent.spawn(check))
        for greenlet in greenlets:
            greenlet.join()
        for i, appid in enumerate(good_app_ids):
            domain = 'goagent%s' % (i + 1)
            LOGGER.info('%s => %s' % (domain, appid))
            subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 60 --replace' % (domain, appid), shell=True)
            time.sleep(0.5)
        LOGGER.info('%s done' % datetime.datetime.now())
        time.sleep(60)



if '__main__' == __name__:
    main()
