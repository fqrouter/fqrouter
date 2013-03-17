import os
import logging

import tornado.web


LOGGER = logging.getLogger(__name__)
SDIO_DEVICE_PATH = '/sys/bus/sdio/devices/mmc2\:0001\:1/device'


class WifiHandler(tornado.web.RequestHandler):
    def post(self):
        action = self.get_argument('action')
        if 'start-hotspot' == action:
            try:
                start_hotspot()
            except Exception, e:
                LOGGER.exception('failed to start hotspot')
                self.write(e.message)


def start_hotspot():
    if not os.path.exists(SDIO_DEVICE_PATH):
        raise Exception('wifi chipset unknown: path to sdio device not found')
    with open(SDIO_DEVICE_PATH) as f:
        wifi_chipset = f.read()
        if '0x4330' != wifi_chipset:
            raise Exception('wifi chipset is not supported: %s' % wifi_chipset)
    raise Exception('feature in development')
