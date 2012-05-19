# *Guesser* works with *Scanner* to help establish TCP connection:
# Send SYN out through via Scanner using UDP
# Receive SYN+ACK from remote *Server* (The one Client is making request to)
# Know the external ip and port used by the connection
from . import output
from . import input
import thread

def start(external_ips):
    output_monitoring_thread = thread.start_new_thread(output.monitor_output, (), dict(
        external_ips=external_ips,
        queue_number=1))
    input_monitoring_thread = thread.start_new_thread(input.monitor_input, (), dict(
        queue_number=2))
    output_monitoring_thread.join()
    input_monitoring_thread.join()

