#!/usr/bin/env python
import sys
import os
import random

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
import gevent
import gevent.monkey
import gevent.queue
import traceback
import fqsocks.goagent
import socket

T1_APP_IDS = ['freegoagent%03d' % i for i in range(1, 1000)]
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

for i in range(1, 60):
    T2_APP_IDS.append('goagentbiz%s' % i)

if len(sys.argv) > 1:
    T1_APP_IDS = [sys.argv[1]]
    T2_APP_IDS = []

random.shuffle(T1_APP_IDS)

APP_ID_QUEUE = gevent.queue.Queue(items=T1_APP_IDS + T2_APP_IDS)
good_app_ids_count = 0


class FakeClient(object):
    def __init__(self):
        self.host = ''
        self.dst_ip = ''

    def add_resource(self, res):
        pass


class FakeProxy(object):
    def __init__(self, fetch_server):
        self.fetch_server = fetch_server


def check():
    global good_app_ids_count
    while True:
        if good_app_ids_count >= 10:
            return
        appid = APP_ID_QUEUE.get()
        try:
            app_status = fqsocks.goagent.gae_urlfetch(
                FakeClient(), FakeProxy('https://%s.appspot.com/2?' % appid),
                'GET', 'http://www.baidu.com', {}, '').app_status
            sys.stderr.write('%s => %s\n' % (appid, app_status))
            sys.stderr.flush()
            if app_status == 200:
                if good_app_ids_count >= 10:
                    return
                print(appid)
                good_app_ids_count += 1
        except:
            sys.stderr.write(traceback.format_exc())
            sys.stderr.flush()

def main():
    gevent.monkey.patch_all()
    fqsocks.goagent.GoAgentProxy.GOOGLE_IPS = socket.gethostbyname_ex('goagent-google-ip.fqrouter.com')[2]
    greenlets = []
    for i in range(8):
        greenlets.append(gevent.spawn(check))
    for greenlet in greenlets:
        greenlet.join()
    print('')



if '__main__' == __name__:
    main()
