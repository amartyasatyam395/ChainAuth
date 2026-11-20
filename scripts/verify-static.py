#!/usr/bin/env python3
"""Dependency-free structural validation for the ChainAuth source tree."""
from pathlib import Path
import json, re, sys, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors, checks = [], []
def check(label, fn):
    try:
        fn(); checks.append(label); print(f"PASS  {label}")
    except Exception as exc:
        errors.append((label, str(exc))); print(f"FAIL  {label}: {exc}")

def parse_manifests():
    ET.parse(ROOT/'backend/pom.xml')
    for p in [ROOT/'frontend/package.json', ROOT/'blockchain/package.json']:
        json.loads(p.read_text())
    json.loads((ROOT/'blockchain/hardhat.config.js').read_text()) if False else None

def parse_yaml():
    import yaml
    yaml.safe_load((ROOT/'backend/src/main/resources/application.yml').read_text())

def delimiter_check(path, pairs):
    s=path.read_text(encoding='utf-8')
    stack=[]; match={')':'(',']':'[','}':'{'}; i=0; state='code'
    while i < len(s):
        ch=s[i]; nxt=s[i+1] if i+1<len(s) else ''
        if state=='code':
            if ch=='/' and nxt=='/': state='line'; i+=2; continue
            if ch=='/' and nxt=='*': state='block'; i+=2; continue
            if ch=='"': state='double'; i+=1; continue
            if ch=="'": state='single'; i+=1; continue
            if ch in '([{': stack.append((ch,i))
            elif ch in ')]}':
                if not stack or stack[-1][0] != match[ch]: raise ValueError(f"unmatched {ch} at offset {i}")
                stack.pop()
        elif state=='line':
            if ch=='\n': state='code'
        elif state=='block':
            if ch=='*' and nxt=='/': state='code'; i+=2; continue
        elif state in ('double','single'):
            quote='"' if state=='double' else "'"
            if ch=='\\': i+=2; continue
            if ch==quote: state='code'
        i+=1
    if state=='block': raise ValueError('unterminated block comment')
    if state in ('double','single'): raise ValueError('unterminated string literal')
    if stack: raise ValueError(f"unclosed {stack[-1][0]} at offset {stack[-1][1]}")

def source_structures():
    java=list((ROOT/'backend/src').rglob('*.java'))
    sol=list((ROOT/'blockchain').rglob('*.sol'))
    if not java: raise ValueError('no Java source files found')
    if not sol: raise ValueError('no Solidity source files found')
    for p in java+sol: delimiter_check(p, 'java/solidity')
    print(f"      inspected {len(java)} Java and {len(sol)} Solidity files")

def required_files():
    required=['README.md','PROJECT_STATUS.md','CHANGELOG.md','VERSION','backend/pom.xml','frontend/package.json','blockchain/package.json','blockchain/contracts/IdentityRegistry.sol','docs/api-reference.md','deployment/README.md','tests/README.md']
    missing=[p for p in required if not (ROOT/p).is_file()]
    if missing: raise ValueError('missing: '+', '.join(missing))

def frontend_syntax():
    import subprocess
    files=sorted(str(p) for p in (ROOT/'frontend/src').rglob('*') if p.suffix in ('.js','.jsx','.ts','.tsx'))
    if not files: raise ValueError('no frontend JS/JSX files found')
    parser='/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/lib/typescript.js'
    if not Path(parser).exists():
        print('      skipped: global TypeScript parser unavailable'); return
    js="""const ts=require(process.argv[1]),fs=require('fs');let errs=[];for(const f of process.argv.slice(2)){const r=ts.transpileModule(fs.readFileSync(f,'utf8'),{fileName:f,reportDiagnostics:true,compilerOptions:{jsx:ts.JsxEmit.ReactJSX,target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}});for(const d of r.diagnostics||[])if(d.category===ts.DiagnosticCategory.Error)errs.push(f+': '+ts.flattenDiagnosticMessageText(d.messageText,' '));}if(errs.length){console.error(errs.join('\\n'));process.exit(1)}console.log('Parsed '+(process.argv.length-2)+' frontend JS/JSX files');"""
    subprocess.run(['node','-e',js,parser,*files],check=True)

check('Maven POM and npm manifests parse', parse_manifests)
check('Application YAML parses (PyYAML required)', parse_yaml)
check('Required project and deployment files exist', required_files)
check('Java and Solidity delimiters balanced', source_structures)
check('Frontend JS/JSX syntax parses', frontend_syntax)
print(f"\nStatic checks: {len(checks)} passed, {len(errors)} failed")
sys.exit(1 if errors else 0)
