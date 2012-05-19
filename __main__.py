import scout
import guesser

external_ips = scout.detect_nat_status([
    'provserver.televolution.net',
    'sip1.lakedestiny.cordiaip.com',
    'stun1.voiceeclipse.net',
    'stun.callwithus.com'
])
guesser.start(external_ips)