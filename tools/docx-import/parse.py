import re,sys,json,html
x=open(sys.argv[1],encoding='utf8').read()
body=x[x.find('<w:body>'):]
def text(s):
    s=re.sub(r'<w:tab/>','<w:t> </w:t>',s)
    s=re.sub(r'<w:(?:br|cr)/>','<w:t> </w:t>',s)
    return html.unescape(''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>',s))).strip()
# tokenize top-level paragraphs and tables (tables not nested assumed)
out=[];pos=0
pat=re.compile(r'<w:tbl>.*?</w:tbl>|<w:p[ >].*?</w:p>',re.S)
for m in pat.finditer(body):
    s=m.group(0)
    if s.startswith('<w:tbl>'):
        rows=[]
        for r in re.findall(r'<w:tr[ >].*?</w:tr>',s,re.S):
            cells=[]
            for c in re.findall(r'<w:tc>.*?</w:tc>',r,re.S):
                span=re.search(r'<w:gridSpan w:val="(\d+)"',c)
                vm=re.search(r'<w:vMerge(?: w:val="(\w+)")?/>',c)
                t=' '.join(text(p) for p in re.findall(r'<w:p[ >].*?</w:p>',c,re.S)).strip()
                t=re.sub(r'\s+',' ',t)
                cells.append({'t':t,'span':int(span.group(1)) if span else 1,'vm':(vm.group(1) or 'cont') if vm else None})
            rows.append(cells)
        out.append({'type':'table','rows':rows})
    else:
        t=re.sub(r'\s+',' ',text(s))
        if t: out.append({'type':'p','t':t})
json.dump(out,open(sys.argv[2],'w',encoding='utf8'),ensure_ascii=False,indent=0)
print(len(out), sum(1 for o in out if o['type']=='table'))
