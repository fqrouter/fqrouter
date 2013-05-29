#!/usr/bin/env python
import sys
import os
import random

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
import urllib2
import functools
import httplib
import gevent
import gevent.monkey
import gevent.pool
import gevent.event
import subprocess
import signal
import atexit
from fqsocks import fqsocks
from fqsocks import goagent

APP_IDS = ['freegoagent001', 'freegoagent002', 'freegoagent003', 'freegoagent004', 'freegoagent005', 'freegoagent006',
           'freegoagent007', 'freegoagent008', 'freegoagent009', 'freegoagent010', 'freegoagent011', 'freegoagent012',
           'freegoagent013', 'freegoagent014', 'freegoagent015', 'freegoagent016', 'freegoagent017', 'freegoagent018',
           'freegoagent019', 'freegoagent020', 'freegoagent021', 'freegoagent022', 'freegoagent023', 'freegoagent024',
           'freegoagent025', 'freegoagent026', 'freegoagent027', 'freegoagent028', 'freegoagent029', 'freegoagent030',
           'freegoagent031', 'freegoagent032', 'freegoagent033', 'freegoagent034', 'freegoagent035', 'freegoagent036',
           'freegoagent037', 'freegoagent038', 'freegoagent039', 'freegoagent040', 'freegoagent041', 'freegoagent042',
           'freegoagent043', 'freegoagent044', 'freegoagent045', 'freegoagent046', 'freegoagent047', 'freegoagent048',
           'freegoagent049', 'freegoagent050', 'freegoagent051', 'freegoagent052', 'freegoagent053', 'freegoagent054',
           'freegoagent055', 'freegoagent056', 'freegoagent057', 'freegoagent058', 'freegoagent059', 'freegoagent060',
           'freegoagent061', 'freegoagent062', 'freegoagent063', 'freegoagent064', 'freegoagent065', 'freegoagent066',
           'freegoagent067', 'freegoagent068', 'freegoagent069', 'freegoagent070', 'freegoagent071', 'freegoagent072',
           'freegoagent073', 'freegoagent074', 'freegoagent075', 'freegoagent076', 'freegoagent077', 'freegoagent078',
           'freegoagent079', 'freegoagent080', 'freegoagent081', 'freegoagent082', 'freegoagent083', 'freegoagent084',
           'freegoagent085', 'freegoagent086', 'freegoagent087', 'freegoagent088', 'freegoagent089', 'freegoagent090',
           'freegoagent091', 'freegoagent092', 'freegoagent093', 'freegoagent094', 'freegoagent095', 'freegoagent096',
           'freegoagent097', 'freegoagent098', 'freegoagent099', 'freegoagent100', 'freegoagent101', 'freegoagent102',
           'freegoagent103', 'freegoagent104', 'freegoagent105', 'freegoagent106', 'freegoagent107', 'freegoagent108',
           'freegoagent109', 'freegoagent110', 'freegoagent121', 'freegoagent122', 'freegoagent124', 'freegoagent125',
           'freegoagent126', 'freegoagent127', 'freegoagent128', 'freegoagent129', 'freegoagent130', 'freegoagent131',
           'freegoagent132', 'freegoagent133', 'freegoagent134', 'freegoagent135', 'freegoagent136', 'freegoagent137',
           'freegoagent138', 'freegoagent139', 'freegoagent140', 'freegoagent141', 'freegoagent142', 'freegoagent143',
           'freegoagent144', 'freegoagent145', 'freegoagent146', 'freegoagent147', 'freegoagent148', 'freegoagent149',
           'freegoagent150', 'freegoagent151', 'freegoagent152', 'freegoagent153', 'freegoagent154', 'freegoagent155',
           'freegoagent156', 'freegoagent157', 'freegoagent158', 'freegoagent159', 'freegoagent160', 'freegoagent161',
           'freegoagent162', 'freegoagent163', 'freegoagent164', 'freegoagent165', 'freegoagent166', 'freegoagent167',
           'freegoagent168', 'freegoagent169', 'freegoagent170', 'freegoagent171', 'freegoagent172', 'freegoagent173',
           'freegoagent174', 'freegoagent175', 'freegoagent176', 'freegoagent177', 'freegoagent178', 'freegoagent179',
           'freegoagent180', 'freegoagent181', 'freegoagent182', 'freegoagent183', 'freegoagent184', 'freegoagent185',
           'freegoagent186', 'freegoagent187', 'freegoagent188', 'freegoagent189', 'freegoagent190', 'freegoagent191',
           'freegoagent192', 'freegoagent193', 'freegoagent194', 'freegoagent195', 'freegoagent196', 'freegoagent197',
           'freegoagent198', 'freegoagent199', 'freegoagent200', 'freegoagent201', 'freegoagent202', 'freegoagent203',
           'freegoagent204', 'freegoagent205', 'freegoagent206', 'freegoagent207', 'freegoagent208', 'freegoagent209',
           'freegoagent210', 'freegoagent211', 'freegoagent212', 'freegoagent213', 'freegoagent214', 'freegoagent215',
           'freegoagent216', 'freegoagent217', 'freegoagent218', 'freegoagent219', 'freegoagent220', 'freegoagent221',
           'freegoagent222', 'freegoagent223', 'freegoagent224', 'freegoagent225', 'freegoagent226', 'freegoagent227',
           'freegoagent228', 'freegoagent229', 'freegoagent230', 'freegoagent231', 'freegoagent232', 'freegoagent233',
           'freegoagent234', 'freegoagent235', 'freegoagent236', 'freegoagent237', 'freegoagent238', 'freegoagent239',
           'freegoagent240', 'freegoagent241', 'freegoagent242', 'freegoagent243', 'freegoagent244', 'freegoagent245',
           'freegoagent246', 'freegoagent247', 'freegoagent248', 'freegoagent249', 'freegoagent250', 'freegoagent251',
           'freegoagent252', 'freegoagent253', 'freegoagent254', 'freegoagent255', 'freegoagent256', 'freegoagent257',
           'freegoagent258', 'freegoagent259', 'freegoagent260', 'freegoagent261', 'freegoagent262', 'freegoagent263',
           'freegoagent264', 'freegoagent265', 'freegoagent266', 'freegoagent267', 'freegoagent268', 'freegoagent269',
           'freegoagent270', 'freegoagent271', 'freegoagent272', 'freegoagent273', 'freegoagent274', 'freegoagent275',
           'freegoagent276', 'freegoagent277', 'freegoagent278', 'freegoagent279', 'freegoagent280', 'freegoagent281',
           'freegoagent282', 'freegoagent283', 'freegoagent284', 'freegoagent285', 'freegoagent286', 'freegoagent287',
           'freegoagent288', 'freegoagent289', 'freegoagent290', 'freegoagent291', 'freegoagent292', 'freegoagent293',
           'freegoagent294', 'freegoagent295', 'freegoagent296', 'freegoagent297', 'freegoagent298', 'freegoagent299',
           'freegoagent300', 'freegoagent301', 'freegoagent302', 'freegoagent303', 'freegoagent304', 'freegoagent305',
           'freegoagent306', 'freegoagent307', 'freegoagent308', 'freegoagent309', 'freegoagent310', 'freegoagent311',
           'freegoagent312', 'freegoagent313', 'freegoagent314', 'freegoagent315', 'freegoagent316', 'freegoagent317',
           'freegoagent318', 'freegoagent319', 'freegoagent320', 'freegoagent321', 'freegoagent322', 'freegoagent323',
           'freegoagent324', 'freegoagent325', 'freegoagent326', 'freegoagent327', 'freegoagent328', 'freegoagent329',
           'freegoagent330', 'freegoagent331', 'freegoagent332', 'freegoagent333', 'freegoagent334', 'freegoagent335',
           'freegoagent336', 'freegoagent337', 'freegoagent338', 'freegoagent339', 'freegoagent340', 'freegoagent341',
           'freegoagent342', 'freegoagent343', 'freegoagent344', 'freegoagent345', 'freegoagent346', 'freegoagent347',
           'freegoagent348', 'freegoagent349', 'freegoagent350', 'freegoagent351', 'freegoagent352', 'freegoagent353',
           'freegoagent354', 'freegoagent355', 'freegoagent356', 'freegoagent357', 'freegoagent358', 'freegoagent359',
           'freegoagent360', 'freegoagent361', 'freegoagent362', 'freegoagent363', 'freegoagent364', 'freegoagent365',
           'freegoagent366', 'freegoagent367', 'freegoagent368', 'freegoagent369', 'freegoagent370', 'freegoagent371',
           'freegoagent372', 'freegoagent373', 'freegoagent374', 'freegoagent375', 'freegoagent376', 'freegoagent377',
           'freegoagent378', 'freegoagent379', 'freegoagent380', 'freegoagent381', 'freegoagent382', 'freegoagent383',
           'freegoagent384', 'freegoagent385', 'freegoagent386', 'freegoagent387', 'freegoagent388', 'freegoagent389',
           'freegoagent390', 'freegoagent391', 'freegoagent392', 'freegoagent393', 'freegoagent394', 'freegoagent395',
           'freegoagent396', 'freegoagent397', 'freegoagent398', 'freegoagent399', 'freegoagent400', 'freegoagent401',
           'freegoagent402', 'freegoagent403', 'freegoagent404', 'freegoagent405', 'freegoagent406', 'freegoagent407',
           'freegoagent408', 'freegoagent409', 'freegoagent410', 'freegoagent411', 'freegoagent412', 'freegoagent413',
           'freegoagent414', 'freegoagent415', 'freegoagent416', 'freegoagent417', 'freegoagent418', 'freegoagent419',
           'freegoagent420', 'freegoagent421', 'freegoagent422', 'freegoagent423', 'freegoagent424', 'freegoagent425',
           'freegoagent426', 'freegoagent427', 'freegoagent428', 'freegoagent429', 'freegoagent430', 'freegoagent431',
           'freegoagent432', 'freegoagent433', 'freegoagent434', 'freegoagent435', 'freegoagent436', 'freegoagent437',
           'freegoagent438', 'freegoagent439', 'freegoagent440', 'freegoagent441', 'freegoagent442', 'freegoagent443',
           'freegoagent444', 'freegoagent445', 'freegoagent446', 'freegoagent447', 'freegoagent448', 'freegoagent449',
           'freegoagent450', 'freegoagent451', 'freegoagent452', 'freegoagent453', 'freegoagent454', 'freegoagent455',
           'freegoagent456', 'freegoagent457', 'freegoagent458', 'freegoagent459', 'freegoagent460', 'freegoagent461',
           'freegoagent462', 'freegoagent463', 'freegoagent464', 'freegoagent465', 'freegoagent466', 'freegoagent467',
           'freegoagent468', 'freegoagent469', 'freegoagent470', 'freegoagent471', 'freegoagent472', 'freegoagent473',
           'freegoagent474', 'freegoagent475', 'freegoagent476', 'freegoagent477', 'freegoagent478', 'freegoagent479',
           'freegoagent480', 'freegoagent481', 'freegoagent482', 'freegoagent483', 'freegoagent484', 'freegoagent485',
           'freegoagent486', 'freegoagent487', 'freegoagent488', 'freegoagent489', 'freegoagent490', 'freegoagent491',
           'freegoagent492', 'freegoagent493', 'freegoagent494', 'freegoagent495', 'freegoagent496', 'freegoagent497',
           'freegoagent498', 'freegoagent499', 'freegoagent500', 'freegoagent501', 'freegoagent502', 'freegoagent503',
           'freegoagent504', 'freegoagent505', 'freegoagent506', 'freegoagent507', 'freegoagent508', 'freegoagent509',
           'freegoagent510', 'freegoagent511', 'freegoagent512', 'freegoagent513', 'freegoagent514', 'freegoagent515',
           'freegoagent516', 'freegoagent517', 'freegoagent518', 'freegoagent519', 'freegoagent520', 'freegoagent521',
           'freegoagent522', 'freegoagent523', 'freegoagent524', 'freegoagent525', 'freegoagent526', 'freegoagent527',
           'freegoagent528', 'freegoagent529', 'freegoagent530', 'freegoagent531', 'freegoagent532', 'freegoagent533',
           'freegoagent534', 'freegoagent535', 'freegoagent536', 'freegoagent537', 'freegoagent538', 'freegoagent539',
           'freegoagent540', 'freegoagent541', 'freegoagent542', 'freegoagent543', 'freegoagent544', 'freegoagent545',
           'freegoagent546', 'freegoagent547', 'freegoagent548', 'freegoagent549', 'freegoagent550', 'freegoagent551',
           'freegoagent552', 'freegoagent553', 'freegoagent554', 'freegoagent555', 'freegoagent556', 'freegoagent557',
           'freegoagent558', 'freegoagent559', 'freegoagent560', 'freegoagent561', 'freegoagent562', 'freegoagent563',
           'freegoagent564', 'freegoagent565', 'freegoagent566', 'freegoagent567', 'freegoagent568', 'freegoagent569',
           'freegoagent570', 'fgabootstrap001', 'fgabootstrap002', 'fgabootstrap003', 'fgabootstrap004',
           'fgabootstrap005', 'fgabootstrap006', 'fgabootstrap007', 'fgabootstrap008', 'fgabootstrap009',
           'fgabootstrap010', 'fgaupdate001', 'fgaupdate002', 'fgaupdate003', 'fgaupdate004', 'fgaupdate005',
           'fgaupdate006', 'fgaupdate007', 'fgaupdate008', 'fgaupdate009', 'fgaupdate010', 'fganr001', 'fganr002'] + [
              'wwqgtxxproxy-1', 'wwqgtxxproxy-2', 'wwqgtxxproxy-3', 'wwqgtxxproxy-4', 'wwqgtxxproxy-5',
              'wwqgtxxproxy-6',
              'wwqgtxxproxy-7', 'wwqgtxxproxy-8', 'wwqgtxxproxy-9', 'wwqgtxxproxy-10', 'wwqgtxxproxy1-1',
              'wwqgtxxproxy1-2',
              'wwqgtxxproxy1-3', 'wwqgtxxproxy1-4', 'wwqgtxxproxy1-5', 'wwqgtxxproxy1-6', 'wwqgtxxproxy1-7',
              'wwqgtxxproxy1-8',
              'wwqgtxxproxy1-9', 'wwqgtxxproxy1-10', 'wwqgtxxproxy2-1', 'wwqgtxxproxy2-2', 'wwqgtxxproxy2-3',
              'wwqgtxxproxy2-4',
              'wwqgtxxproxy2-5', 'wwqgtxxproxy2-6', 'wwqgtxxproxy2-7', 'wwqgtxxproxy2-8', 'wwqgtxxproxy2-9',
              'wwqgtxxproxy2-10',
              'wwqgtxxproxy3-1', 'wwqgtxxproxy3-2', 'wwqgtxxproxy3-3', 'wwqgtxxproxy3-4', 'wwqgtxxproxy3-5',
              'wwqgtxxproxy3-6',
              'wwqgtxxproxy3-7', 'wwqgtxxproxy3-8', 'wwqgtxxproxy3-9', 'wwqgtxxproxy3-10', 'wwqgtxxproxy4-1',
              'wwqgtxxproxy4-2',
              'wwqgtxxproxy4-3', 'wwqgtxxproxy4-4', 'wwqgtxxproxy4-5', 'wwqgtxxproxy4-6', 'wwqgtxxproxy4-7',
              'wwqgtxxproxy4-8',
              'wwqgtxxproxy4-9', 'wwqgtxxproxy4-10', 'wwqgtxxproxy5-1', 'wwqgtxxproxy5-2', 'wwqgtxxproxy5-3',
              'wwqgtxxproxy5-4',
              'wwqgtxxproxy5-5', 'wwqgtxxproxy5-6', 'wwqgtxxproxy5-7', 'wwqgtxxproxy5-8', 'wwqgtxxproxy5-9',
              'wwqgtxxproxy5-10',
              'wwqgtxxproxy6-1', 'wwqgtxxproxy6-2', 'wwqgtxxproxy6-3', 'wwqgtxxproxy6-4', 'wwqgtxxproxy6-5',
              'wwqgtxxproxy6-6',
              'wwqgtxxproxy6-7', 'wwqgtxxproxy6-8', 'wwqgtxxproxy6-9', 'wwqgtxxproxy6-10', 'wwqgtxxproxy7-1',
              'wwqgtxxproxy7-2',
              'wwqgtxxproxy7-3', 'wwqgtxxproxy7-4', 'wwqgtxxproxy7-5', 'wwqgtxxproxy7-6', 'wwqgtxxproxy7-7',
              'wwqgtxxproxy7-8',
              'wwqgtxxproxy7-9', 'wwqgtxxproxy7-10', 'wwqgtxxproxy8-1', 'wwqgtxxproxy8-2', 'wwqgtxxproxy8-3',
              'wwqgtxxproxy8-4',
              'wwqgtxxproxy8-5', 'wwqgtxxproxy8-6', 'wwqgtxxproxy8-7', 'wwqgtxxproxy8-8', 'wwqgtxxproxy8-9',
              'wwqgtxxproxy8-10',
              'wwqgtxxproxy9-1', 'wwqgtxxproxy9-2', 'wwqgtxxproxy9-3', 'wwqgtxxproxy9-4', 'wwqgtxxproxy9-5',
              'wwqgtxxproxy9-6',
              'wwqgtxxproxy9-7', 'wwqgtxxproxy9-8', 'wwqgtxxproxy9-9', 'wwqgtxxproxy9-10', 'wwqgtxxproxy10-1',
              'wwqgtxxproxy10-2',
              'wwqgtxxproxy10-3', 'wwqgtxxproxy10-4', 'wwqgtxxproxy10-5', 'wwqgtxxproxy10-6', 'wwqgtxxproxy10-7',
              'wwqgtxxproxy10-8', 'wwqgtxxproxy10-9', 'wwqgtxxproxy10-10', 'wwqgtxxproxy11-1', 'wwqgtxxproxy11-2',
              'wwqgtxxproxy11-3', 'wwqgtxxproxy11-4', 'wwqgtxxproxy11-5', 'wwqgtxxproxy11-6', 'wwqgtxxproxy11-7',
              'wwqgtxxproxy11-8', 'wwqgtxxproxy11-9', 'wwqgtxxproxy11-10', 'wwqgtxxproxy12-1', 'wwqgtxxproxy12-2',
              'wwqgtxxproxy12-3', 'wwqgtxxproxy12-4', 'wwqgtxxproxy12-5', 'wwqgtxxproxy12-6', 'wwqgtxxproxy12-7',
              'wwqgtxxproxy12-8', 'wwqgtxxproxy12-9', 'wwqgtxxproxy12-10', 'wwqgtxxproxy13-1', 'wwqgtxxproxy13-2',
              'wwqgtxxproxy13-3', 'wwqgtxxproxy13-4', 'wwqgtxxproxy13-5', 'wwqgtxxproxy13-6', 'wwqgtxxproxy13-7',
              'wwqgtxxproxy13-8', 'wwqgtxxproxy13-9', 'wwqgtxxproxy13-10', 'wwqgtxxproxy14-1', 'wwqgtxxproxy14-2',
              'wwqgtxxproxy14-3', 'wwqgtxxproxy14-4', 'wwqgtxxproxy14-5', 'wwqgtxxproxy14-6', 'wwqgtxxproxy14-7',
              'wwqgtxxproxy14-8', 'wwqgtxxproxy14-9', 'wwqgtxxproxy14-10', 'wwqgtxxproxy15-1', 'wwqgtxxproxy15-2',
              'wwqgtxxproxy15-3', 'wwqgtxxproxy15-4', 'wwqgtxxproxy15-5', 'wwqgtxxproxy15-6', 'wwqgtxxproxy15-7',
              'wwqgtxxproxy15-8', 'wwqgtxxproxy15-9', 'wwqgtxxproxy15-10', 'wwqgtxxproxy16-1', 'wwqgtxxproxy16-2',
              'wwqgtxxproxy16-3', 'wwqgtxxproxy16-4', 'wwqgtxxproxy16-5', 'wwqgtxxproxy16-6', 'wwqgtxxproxy16-7',
              'wwqgtxxproxy16-8', 'wwqgtxxproxy16-9', 'wwqgtxxproxy16-10', 'wwqgtxxproxy17-1', 'wwqgtxxproxy17-2',
              'wwqgtxxproxy17-3', 'wwqgtxxproxy17-4', 'wwqgtxxproxy17-5', 'wwqgtxxproxy17-6', 'wwqgtxxproxy17-7',
              'wwqgtxxproxy17-8', 'wwqgtxxproxy17-9', 'wwqgtxxproxy17-10', 'wwqgtxxproxy18-1', 'wwqgtxxproxy18-2',
              'wwqgtxxproxy18-3', 'wwqgtxxproxy18-4', 'wwqgtxxproxy18-5', 'wwqgtxxproxy18-6', 'wwqgtxxproxy18-7',
              'wwqgtxxproxy18-8', 'wwqgtxxproxy18-9', 'wwqgtxxproxy18-10', 'wwqgtxxproxy19-1', 'wwqgtxxproxy19-2',
              'wwqgtxxproxy19-3', 'wwqgtxxproxy19-4', 'wwqgtxxproxy19-5', 'wwqgtxxproxy19-6', 'wwqgtxxproxy19-7',
              'wwqgtxxproxy19-8', 'wwqgtxxproxy19-9', 'wwqgtxxproxy19-10', 'wwqgtxxproxy20-1', 'wwqgtxxproxy20-2',
              'wwqgtxxproxy20-3', 'wwqgtxxproxy20-4', 'wwqgtxxproxy20-5', 'wwqgtxxproxy20-6', 'wwqgtxxproxy20-7',
              'wwqgtxxproxy20-8', 'wwqgtxxproxy20-9', 'wwqgtxxproxy20-10', 'wwqgtxxproxy21-1', 'wwqgtxxproxy21-2',
              'wwqgtxxproxy21-3', 'wwqgtxxproxy21-4', 'wwqgtxxproxy21-5', 'wwqgtxxproxy21-6', 'wwqgtxxproxy21-7',
              'wwqgtxxproxy21-8', 'wwqgtxxproxy21-9', 'wwqgtxxproxy21-10', 'wwqgtxxproxy22-1', 'wwqgtxxproxy22-2',
              'wwqgtxxproxy22-3', 'wwqgtxxproxy22-4', 'wwqgtxxproxy22-5', 'wwqgtxxproxy22-6', 'wwqgtxxproxy22-7',
              'wwqgtxxproxy22-8', 'wwqgtxxproxy22-9', 'wwqgtxxproxy22-10', 'wwqgtxxproxy23-1', 'wwqgtxxproxy23-2',
              'wwqgtxxproxy23-3', 'wwqgtxxproxy23-4', 'wwqgtxxproxy23-5', 'wwqgtxxproxy23-6', 'wwqgtxxproxy23-7',
              'wwqgtxxproxy23-8', 'wwqgtxxproxy23-9', 'wwqgtxxproxy23-10', 'wwqgtxxproxy24-1', 'wwqgtxxproxy24-2',
              'wwqgtxxproxy24-3', 'wwqgtxxproxy24-4', 'wwqgtxxproxy24-5', 'wwqgtxxproxy24-6', 'wwqgtxxproxy24-7',
              'wwqgtxxproxy24-8', 'wwqgtxxproxy24-9', 'wwqgtxxproxy24-10', 'wwqgtxxproxy25-1', 'wwqgtxxproxy25-2',
              'wwqgtxxproxy25-3', 'wwqgtxxproxy25-4', 'wwqgtxxproxy25-5', 'wwqgtxxproxy25-6', 'wwqgtxxproxy25-7',
              'wwqgtxxproxy25-8', 'wwqgtxxproxy25-9', 'wwqgtxxproxy25-10', 'wwqgtxxproxy26-1', 'wwqgtxxproxy26-2',
              'wwqgtxxproxy26-3', 'wwqgtxxproxy26-4', 'wwqgtxxproxy26-5', 'wwqgtxxproxy26-6', 'wwqgtxxproxy26-7',
              'wwqgtxxproxy26-8', 'wwqgtxxproxy26-9', 'wwqgtxxproxy26-10', 'wwqgtxxproxy27-1', 'wwqgtxxproxy27-2',
              'wwqgtxxproxy27-3', 'wwqgtxxproxy27-4', 'wwqgtxxproxy27-5', 'wwqgtxxproxy27-6', 'wwqgtxxproxy27-7',
              'wwqgtxxproxy27-8', 'wwqgtxxproxy27-9', 'wwqgtxxproxy27-10', 'wwqgtxxproxy28-1', 'wwqgtxxproxy28-2',
              'wwqgtxxproxy28-3', 'wwqgtxxproxy28-4', 'wwqgtxxproxy28-5', 'wwqgtxxproxy28-6', 'wwqgtxxproxy28-7',
              'wwqgtxxproxy28-8', 'wwqgtxxproxy28-9', 'wwqgtxxproxy28-10', 'fanyueproxy1-01', 'fanyueproxy1-02',
              'fanyueproxy1-03', 'fanyueproxy1-04', 'fanyueproxy1-05'] + [
              'vi88com1', 'vi88com10', 'vi88com 11', 'vi88com2', 'vi88com12', 'vi88com3', 'vi88com13', 'vi88com4',
              'vi88com14', 'vi88com5', 'vi88com15', 'vi88com6', 'vi88com16', 'vi88com7', 'vi88com17', 'vi88com8',
              'vi88com18',
              'vi88com19', 'vip6xlgonggongid01', 'gongongid02', 'gonggongid03', 'gonggongid04', 'gonggongid05',
              'gonggongid06',
              'gonggongid07', 'gonggongid08', 'gonggongid09', 'gonggongid10', 'goagent-dup001', 'goagent-dup002',
              'goagent-dup003', 'gonggongid11', 'gonggongid12', 'gonggongid13', 'gonggongid14', 'gonggongid15',
              'gonggongid16', 'gonggongid17', 'gonggongid18', 'gonggongid19', 'gonggongid20', 'gfwsbgfwsbgfwsb',
              '1.sbgfwsbgfwsbgfw', '1.wyq476137265', '1.wangyuqi19961213', 'xinxijishuwyq21', 'xinxijishuwyq22',
              'xinxijishuwyq23', 'xinxijishuwyq24', 'xinxijishuwyq25']

if len(sys.argv) > 1:
    APP_IDS = [sys.argv[1]]

random.shuffle(APP_IDS)
GOOD_APP_IDS = set()
done = gevent.event.Event()


class BoundHTTPHandler(urllib2.HTTPHandler):
    def __init__(self, source_address=None, debuglevel=0):
        urllib2.HTTPHandler.__init__(self, debuglevel)
        self.http_class = functools.partial(httplib.HTTPConnection, source_address=source_address)

    def http_open(self, req):
        return self.do_open(self.http_class, req)


handler = BoundHTTPHandler(source_address=('10.26.1.100', 0))
opener = urllib2.build_opener(handler)
fqsocks.LISTEN_IP = '127.0.0.1'
fqsocks.LISTEN_PORT = 1100
fqsocks.CHINA_PROXY = None


class CheckingGoAgentProxy(fqsocks.GoAgentProxy):
    def forward(self, client):
        super(CheckingGoAgentProxy, self).forward(client)
        sys.stderr.write('found: ')
        sys.stderr.write(self.appid)
        sys.stderr.write('\n')
        if self.appid not in GOOD_APP_IDS:
            GOOD_APP_IDS.add(self.appid)
            print(self.appid)
            if len(GOOD_APP_IDS) >= 10:
                done.set()
        self.died = True


for appid in APP_IDS:
    fqsocks.mandatory_proxies.append(CheckingGoAgentProxy(appid))


def check_baidu_access():
    try:
        opener.open('http://www.baidu.com').read()
    except:
        pass


def keep_fqsocks_busy():
    goagent.GoAgentProxy.GOOGLE_HOSTS = ['goagent-google-ip.fqrouter.com']
    goagent.GoAgentProxy.refresh(fqsocks.mandatory_proxies, fqsocks.create_udp_socket, fqsocks.create_tcp_socket)
    pool = gevent.pool.Pool(size=4)
    for i in range(100):
        pool.apply_async(check_baidu_access)


def check_if_all_died():
    while True:
        gevent.sleep(1)
        if all(p.died for p in fqsocks.mandatory_proxies):
            done.set()


def setup():
    subprocess.check_call('ifconfig lo:goagent 10.26.1.100 netmask 255.255.255.255', shell=True)
    subprocess.check_call('iptables -t nat -I OUTPUT -s 10.26.1.100 -p tcp -j REDIRECT --to-port 1100', shell=True)


def teardown():
    subprocess.check_call('iptables -t nat -D OUTPUT -s 10.26.1.100 -p tcp -j REDIRECT --to-port 1100', shell=True)


def main():
    signal.signal(signal.SIGTERM, lambda signum, fame: teardown())
    signal.signal(signal.SIGINT, lambda signum, fame: teardown())
    atexit.register(teardown)
    setup()
    gevent.monkey.patch_all(thread=False)
    gevent.spawn(fqsocks.start_server)
    gevent.spawn(keep_fqsocks_busy)
    gevent.spawn(check_if_all_died)
    done.wait()


if '__main__' == __name__:
    main()