import logging
import re
import shlex
import shell

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
RE_CHAIN_NAME = re.compile(r'Chain (.+) \(')
RE_SPACE = re.compile(r'\s+')


def insert_rules(rules, to_fq_chain=True):
    for signature, rule_args in reversed(rules): # insert the last one first
        if to_fq_chain:
            rule_args = update_rule_args(rule_args)
        table, chain, _ = rule_args
        if contains_rule(table, chain, signature):
            LOGGER.info('skip insert rule: -t %s -I %s %s' % rule_args)
        else:
            insert_rule(signature.get('optional'), *rule_args)


def delete_rules(rules):
    for signature, rule_args in rules:
        try:
            rule_args = update_rule_args(rule_args)
            table, chain, _ = rule_args
            for i in range(16):
                if contains_rule(table, chain, signature):
                    delete_rule(*rule_args)
                else:
                    LOGGER.info('skip delete rule: -t %s -D %s %s' % rule_args)
                    break
        except:
            LOGGER.exception('failed to delete rule: -t %s -D %s %s' % rule_args)


def update_rule_args(rule_args):
    rule_args = list(rule_args)
    rule_args[1] = 'fq_%s' % rule_args[1]
    return tuple(rule_args)


def flush_fq_chain():
    for table in ('filter', 'nat'):
        rules = dump_table(table)
        for chain, chain_rules in rules.items():
            if chain.startswith('fq_'):
                shell.call(shlex.split('iptables -t %s --flush %s' % (table, chain)))


def init_fq_chains():
    init_fq_chains_for_table('filter', ['OUTPUT', 'FORWARD', 'INPUT'])
    init_fq_chains_for_table('nat', ['PREROUTING', 'INPUT', 'OUTPUT', 'POSTROUTING'])


def init_fq_chains_for_table(table, chains):
    rules = dump_table(table)
    for chain in chains:
        fq_chain = 'fq_%s' % chain
        if fq_chain not in rules:
            shell.call(shlex.split('iptables -t %s -N %s' % (table, fq_chain)))
        ensure_first_target(table, chain, rules.get(chain, []), fq_chain)


def ensure_first_target(table, from_chain, from_chain_rules, to_chain):
    if not from_chain_rules:
        shell.call(shlex.split('iptables -t %s -I %s -j %s' % (table, from_chain, to_chain)))
        return
    if to_chain == from_chain_rules[0]['target']:
        return
    to_be_deleted = []
    for i, rule in enumerate(from_chain_rules):
        if to_chain == rule['target']:
            to_be_deleted.append(i + 1)
    for i in reversed(to_be_deleted):
        shell.call(shlex.split('iptables -t %s -D %s %s' % (table, from_chain, i)))
    shell.call(shlex.split('iptables -t %s -I %s -j %s' % (table, from_chain, to_chain)))


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
    try:
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
                if len(parts) < 9:
                    LOGGER.error('bad line: %s' % line)
                    continue
                rule['pkts'], rule['bytes'], rule['target'], rule['prot'], rule['opt'], \
                rule['iface_in'], rule['iface_out'], rule['source'], rule['destination'] = parts[:9]
                rule['extra'] = ' '.join(parts[9:])
                LOGGER.debug('parsed rule: %s' % str(rule))
                rules.setdefault(current_chain, []).append(rule)
        return rules
    except:
        LOGGER.exception('failed to parse iptables output: %s' % output)
        raise
