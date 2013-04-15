import os
import logging
import logging.handlers

import tornado.ioloop
import tornado.template
import tornado.web

import dns_service
import tcp_service
import full_proxy_service
import wifi


LOGGER = logging.getLogger(__name__)
ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router'
LOG_FILE = os.path.join(LOG_DIR, 'manager.log')
template_loader = tornado.template.Loader(ROOT_DIR)


class MainHandler(tornado.web.RequestHandler):
    def get(self):
        template = template_loader.load('dashboard.html')
        self.write(template.generate(
            dns_service_status=dns_service.status,
            tcp_service_status=tcp_service.status))


class PingHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('PONG')


class LogsHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('<html><body><pre>')
        lines = []
        with open(LOG_FILE) as f:
            lines.append(f.read())
        for line in reversed(lines):
            self.write(line)
        self.write('</pre></body></html>')


application = tornado.web.Application([
    (r'/static/(.*)', tornado.web.StaticFileHandler, {'path': os.path.join(ROOT_DIR, 'static')}),
    (r'/ping', PingHandler),
    (r'/logs', LogsHandler),
    (r'/wifi', wifi.WifiHandler),
    (r'/', MainHandler)
])


def setup_logging():
    logging.basicConfig(level=logging.INFO)
    handler = logging.handlers.RotatingFileHandler(
        LOG_FILE, maxBytes=1024 * 1024, backupCount=1)
    handler.setFormatter(logging.Formatter(logging.BASIC_FORMAT))
    logging.getLogger().addHandler(handler)


application.listening_to_hotspot_lan = False


def listen_to_hotspot_lan():
    if not application.listening_to_hotspot_lan:
        application.listening_to_hotspot_lan = True
        application.listen(80, '192.168.49.1')


if '__main__' == __name__:
    setup_logging()
    dns_service.run()
    tcp_service.run()
    full_proxy_service.run()
    LOGGER.info('services started')
    application.listen(8318, '127.0.0.1')
    wifi.on_wifi_hotspot_started = listen_to_hotspot_lan
    tornado.ioloop.IOLoop.instance().start()