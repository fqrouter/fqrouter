import os
import thread
import tornado.ioloop
import tornado.template
import tornado.web
import logging
import dns_agent


ROOT_DIR = os.path.dirname(__file__)
template_loader = tornado.template.Loader(ROOT_DIR)


class MainHandler(tornado.web.RequestHandler):
    def get(self):
        template = template_loader.load('dashboard.html')
        self.write(template.generate(dns_agent_status=dns_agent.status))


class PingHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('PONG')


application = tornado.web.Application([
    (r'/ping', PingHandler),
    (r'/', MainHandler)
])

if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    thread.start_new(dns_agent.run, ())
    application.listen(8888, '127.0.0.1')
    tornado.ioloop.IOLoop.instance().start()