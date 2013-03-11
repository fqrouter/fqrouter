import logging
import re
import subprocess
import shlex

LOGGER = logging.getLogger(__name__)
RE_CHAIN_NAME = re.compile(r'Chain (.+) \(')
RE_SPACE = re.compile(r'\s+')

def insert_rules(rules):
    for signature, rule_args in reversed(rules): # insert the last one first
        table, chain, _ = rule_args
        if contains_rule(table, chain, signature):
            LOGGER.info('skip insert rule: -t %s -I %s %s' % rule_args)
        else:
            insert_rule(*rule_args)


def delete_rules(rules):
    for signature, rule_args in rules:
        try:
            table, chain, _ = rule_args
            if contains_rule(table, chain, signature):
                LOGGER.info('skip delete rule: -t %s -D %s %s' % rule_args)
            else:
                delete_rule(*rule_args)
        except:
            LOGGER.exception('failed to delete rule: -t %s -D %s %s' % rule_args)


def insert_rule(table, chain, rule_text):
    command = 'iptables -t %s -I %s %s' % (table, chain, rule_text)
    LOGGER.info('insert rule: %s' % command)
    subprocess.check_call(shlex.split(command))


def delete_rule(table, chain, rule_text):
    command = 'iptables -t %s -D %s %s' % (table, chain, rule_text)
    LOGGER.info('delete rule: %s' % command)
    try:
        subprocess.check_call(shlex.split(command))
    except:
        LOGGER.exception('failed to delete rule: %s' % command)


def contains_rule(table, chain, signature):
    rules = dump_table(table) if isinstance(table, basestring) else table
    rules = rules.get(chain, [])
    for rule in rules:
        signature_parts = set(signature.items())
        if not (signature_parts - set(rule.items())): # all parts matched
            return True
    return False


def dump_table(table):
    command = 'iptables -t %s -L -v -n' % table
    LOGGER.debug('command: %s' % command)
    output = subprocess.check_output(shlex.split(command))
    LOGGER.debug('output: %s' % output)
    return parse(output)


def parse(output):
    current_chain = None
    rules = {}
    lines = iter(output.splitlines(False))
    for line in lines:
        line = line.strip()
        if not line:
            continue
        match = RE_CHAIN_NAME.match(line)
        if match:
            current_chain = match.group(1)
            LOGGER.debug('current_chain: %s' % current_chain)
            lines.next() # skip the line below Chain xxx
            continue
        else:
            if not current_chain:
                LOGGER.error('found rule before chain is identified: %s' % line)
                continue
            parts = RE_SPACE.split(line)
            rule = {}
            rule['pkts'], rule['bytes'], rule['target'], rule['prot'], rule['opt'], \
            rule['iface_in'], rule['iface_out'], rule['source'], rule['destination'] = parts[:9]
            rule['extra'] = ' '.join(parts[9:])
            LOGGER.debug('parsed rule: %s' % str(rule))
            rules.setdefault(current_chain, []).append(rule)
    return rules


if '__main__' == __name__:
    logging.basicConfig(level=logging.DEBUG)
    sample_output = """
Chain PREROUTING (policy ACCEPT 0 packets, 0 bytes)
 pkts bytes target     prot opt in     out     source               destination

Chain OUTPUT (policy ACCEPT 15 packets, 1260 bytes)
 pkts bytes target     prot opt in     out     source               destination
   37  2165 DNAT       udp  --  *      *       0.0.0.0/0            0.0.0.0/0           udp dpt:53 to:8.8.8.8:53

Chain POSTROUTING (policy ACCEPT 52 packets, 3425 bytes)
 pkts bytes target     prot opt in     out     source               destination
 """
    print(parse(sample_output))
