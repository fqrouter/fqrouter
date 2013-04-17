#!/usr/bin/env python
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
import manager.goagent

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
           'fgaupdate006', 'fgaupdate007', 'fgaupdate008', 'fgaupdate009', 'fgaupdate010', 'fganr001', 'fganr002']

if len(sys.argv) > 1:
    APP_IDS = [sys.argv[1]]

count = 0
for app_id in APP_IDS:
    fetchserver = '%s://%s.appspot.com%s?' % \
                  (manager.goagent.common.GOOGLE_MODE, app_id, manager.goagent.common.GAE_PATH)
    app_status = manager.goagent.gae_urlfetch('GET', 'http://www.baidu.com', {}, None, fetchserver).app_status
    if 200 == app_status:
        count += 1
        sys.stderr.write('[OK] %s\n' % app_id)
        print(app_id)
    else:
        sys.stderr.write('[FAILED:%s] %s\n' % (app_status, app_id))
    if count == 10:
        break
print('')