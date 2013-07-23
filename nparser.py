import json
import sys
from pyparsing import *

########################################################
# Globals
########################################################
file_in = 'stamp.txt'
file_out= 'stamp.json'
stamp_dict = {}
dct_typ = ''
key_set = set(['col_mode', 'bin'])

########################################################
# Utilities
########################################################
'''
Create dict once identifies '[something]'
'''
def genDict(str, loc, token):
  global dct_typ
	global stamp_dict
	dct_typ = token[0]
	stamp_dict[dct_typ] = {}

'''
Insert record into Dict
'''
def insert(str, loc, token):
	global dct_typ
	global stamp_dict
	if(dct_typ=='BIN'):
		stamp_dict[dct_typ][token[0]] = token[1].asList()
	else:
		stamp_dict[dct_typ][token[0]] = token[1:]

'''
Check if group name was defined
'''
def hasGrp(str, loc, token):
	global stamp_dict
	if token[0] not in stamp_dict:
		errOut(token[0],'is not defined yet')

'''
Check if keyword is supported
'''
def isKey(str, loc, token):
	global key_set
	if token[0] not in key_set:
		errOut(token[0],'is not supported')

'''
Check if col_st <= col_sp
'''
def chkCol(str, loc, token):
	if int(token[0],16)>int(token[1],16):
		errOut(token[0],'is expected no more than',token[1])

def errOut(*args):
		print >> sys.stderr, '********************** ERROR **********************'
		print >> sys.stderr, ' '.join(args)
		print >> sys.stderr, '***************************************************'
		sys.exit(1)

########################################################
# Parser Definition
########################################################

# Separators & blanklines & comments & k-v
kv_sep  = Suppress('=')
kv_pair = (Word( alphanums+'_-').setParseAction(isKey) + kv_sep + Word( alphanums+'_-')).setParseAction(insert)
blankline =  LineEnd() + OneOrMore(LineStart())

# section name
grp_sec = Combine( Suppress('[') + Word( alphanums+'_-') + Suppress(']') ).setName('GroupName').setParseAction(genDict)
bin_sec = Combine( Suppress('[') + 'BIN' + Suppress(']') ).setName('BinLogic').setParseAction(genDict)
default_sec = Combine( Suppress('[') + 'default' + Suppress(']') ).setName('Default').setParseAction(genDict)

# format of stamp info 
hex_int = Combine(CaselessLiteral('0x') + Word(hexnums)).setName('hex_int')
columns = (hex_int+hex_int).setName('columns')
page    = hex_int.setName('page')
exp_val = hex_int.setName('stamp_exp')
stamp_name  = Word( alphanums+'_-').setName('stamp_name')
stamp_parser= (stamp_name + kv_sep + columns + page + exp_val).setParseAction(insert)

# format of binning logic
bin_num = Combine('bin'+Word(nums)).setName('bin#')
stamp_grp_num = Word( alphanums+'_-').setParseAction(hasGrp)
boolExpr = operatorPrecedence( stamp_grp_num,
			[
			('!', 1, opAssoc.RIGHT),
			('~', 1, opAssoc.RIGHT),
			('&', 2, opAssoc.LEFT),
			('|', 2, opAssoc.LEFT)
			])
bin_parser = (bin_num + kv_sep + boolExpr).setParseAction(insert)

conf_parser = stamp_parser | bin_parser | grp_sec | bin_sec | default_sec | kv_pair | blankline
map(conf_parser.parseString, open(file_in).readlines())

########################################################
# Dump JSON
########################################################
json.dump(stamp_dict, open(file_out,'w'))
print open(file_out).readlines()

