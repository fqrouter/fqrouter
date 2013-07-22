import logging
import re
import shlex
import shell

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
RE_CHAIN_NAME = re.compile(r'Chain (.+) \(')
RE_SPACE = re.compile(r'\s+')


def insert_rules(rules):
    created_chains = set()
    for signature, rule_args in reversed(rules): # insert the last one first
        table, chain, _ = rule_args
        if chain not in ['OUTPUT', 'INPUT', 'FORWARD', 'PREROUTING', 'POSTROUTING'] and chain not in created_chains:
            shell.call(shlex.split('iptables -t %s -N %s' % (table, chain)))
            created_chains.add(chain)
        if contains_rule(table, chain, signature):
            LOGGER.info('skip insert rule: -t %s -I %s %s' % rule_args)
        else:
            insert_rule(signature.get('optional'), *rule_args)


def delete_rules(rules):
    for signature, rule_args in rules:
        try:
            table, chain, _ = rule_args
            for i in range(16):
                if contains_rule(table, chain, signature):
                    delete_rule(*rule_args)
                else:
                    LOGGER.info('skip delete rule: -t %s -D %s %s' % rule_args)
                    break
        except:
            LOGGER.exception('failed to delete rule: -t %s -D %s %s' % rule_args)


def delete_nfqueue_rules(queue_number):
    signature = 'NFQUEUE num %s' % queue_number
    for table in ('filter', 'nat'):
        rules = dump_table(table)
        for chain, chain_rules in rules.items():
            for i, rule in enumerate(reversed(chain_rules)):
                index = len(chain_rules) - i
                if signature in rule['extra']:
                    delete_rule(table, chain, str(index))


def delete_chain(target):
    for table in ('filter', 'nat'):
        rules = dump_table(table)
        for chain, chain_rules in rules.items():
            for i, rule in enumerate(reversed(chain_rules)):
                index = len(chain_rules) - i
                if target == rule['target']:
                    delete_rule(table, chain, str(index))
        shell.call(shlex.split('iptables -t %s --flush %s' % (table, target)))
        shell.call(shlex.split('iptables -t %s -X %s' % (table, target)))


def insert_rule(optional, table, chain, rule_text):
    command = 'iptables -t %s -I %s %s' % (table, chain, rule_text)
    LOGGER.info('insert %s rule: %s' % ('optional' if optional else 'mandatory', command))
    try:
        shell.check_call(shlex.split(command))
    except:
        if optional:
            LOGGER.exception('skip optional iptables rule')
        else:
            raise


def delete_rule(table, chain, rule_text):
    command = 'iptables -t %s -D %s %s' % (table, chain, rule_text)
    LOGGER.info('delete rule: %s' % command)
    try:
        shell.check_call(shlex.split(command))
    except:
        LOGGER.exception('failed to delete rule: %s' % command)


def contains_rule(table, chain, signature):
    signature = dict(signature)
    signature.pop('optional', None)
    rules = dump_table(table) if isinstance(table, basestring) else table
    rules = rules.get(chain, [])
    for rule in rules:
        signature_parts = set(signature.items())
        if not (signature_parts - set(rule.items())): # all parts matched
            return True
        if 'tcpflags: ' in signature.get('extra', ''):
            signature['extra'] = signature['extra'].replace('tcpflags: ', 'tcp flags:')
            signature_parts = set(signature.items())
        if not (signature_parts - set(rule.items())): # all parts matched
            return True
    return False


def dump_table(table):
    command = 'iptables -t %s -L -v -n' % table
    LOGGER.debug('command: %s' % command)
    output = shell.check_output(shlex.split(command))
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
    delete_nfqueue_rules(1)
