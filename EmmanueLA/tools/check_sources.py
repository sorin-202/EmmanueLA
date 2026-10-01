#!/usr/bin/env python3
"""Structural checks only; not a Kotlin compiler or a replacement for Android lint."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
def skip_string(text,i):
    if text.startswith('"""',i):
        end=text.find('"""',i+3)
        if end<0:raise ValueError('Unclosed raw string')
        return end+3
    quote=text[i];i+=1
    while i<len(text):
        if text[i]=='\\':i+=2;continue
        if quote=='"' and text.startswith('${',i):
            depth=1;i+=2
            while i<len(text) and depth:
                if text[i] in '\"\'':i=skip_string(text,i);continue
                if text[i]=='{':depth+=1
                elif text[i]=='}':depth-=1
                i+=1
            continue
        if text[i]==quote:return i+1
        i+=1
    raise ValueError('Unclosed string')
def check_balance(text):
    stack=[];i=0
    while i<len(text):
        if text.startswith('//',i):
            end=text.find('\n',i);i=end if end>=0 else len(text);continue
        if text.startswith('/*',i):
            depth=1;i+=2
            while i<len(text) and depth:
                if text.startswith('/*',i):depth+=1;i+=2
                elif text.startswith('*/',i):depth-=1;i+=2
                else:i+=1
            if depth:raise ValueError('Unclosed comment')
            continue
        if text[i] in '\"\'':i=skip_string(text,i);continue
        c=text[i]
        if c in '([{':stack.append((c,i))
        elif c in ')]}':
            if not stack or stack[-1][0]!={')':'(',']':'[','}':'{'}[c]:raise ValueError(f'Mismatched {c} at line {text[:i].count(chr(10))+1}')
            stack.pop()
        i+=1
    if stack:raise ValueError('Unclosed delimiter at line '+str(text[:stack[-1][1]].count('\n')+1))
errors=[]
files=list((ROOT/'app/src').rglob('*.kt'))
for path in files:
    try:check_balance(path.read_text())
    except ValueError as e:errors.append(f'{path.relative_to(ROOT)}: {e}')
for path in (ROOT/'app/src/main/res').rglob('*.xml'):
    try:ET.parse(path)
    except ET.ParseError as e:errors.append(f'{path.relative_to(ROOT)}: {e}')
manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
classes={re.search(r'^package ([\w.]+)',p.read_text(),re.M)[1]+'.'+name for p in (ROOT/'app/src/main/java').rglob('*.kt') for name in re.findall(r'^(?:open )?class (\w+)',p.read_text(),re.M)}
for element in manifest.iter():
    name=element.get('{http://schemas.android.com/apk/res/android}name','')
    if name.startswith('.') and 'com.emmanuela.launcher'+name not in classes:errors.append('Missing manifest class '+name)
if errors:raise SystemExit('\n'.join(errors))
print(f'PASS: balanced delimiters in {len(files)} Kotlin files; valid XML; manifest classes exist.')
print('Compile, runtime, UI and performance checks still require Android Studio / SDK.')
