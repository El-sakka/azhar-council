"""Generate Flyway seed migrations from the parsed Word document (doc.json)."""
import json, re, sys, os, collections

doc = json.load(open(sys.argv[1], encoding='utf8'))
out_dir = sys.argv[2]

SUBJECTS = [
    ('1', 'إعانات'), ('1/1', 'إعانات الطلاب'), ('1/2', 'إعانات أسر العلماء'),
    ('2', 'امتحانات'), ('2/1', 'التماسات'), ('2/2', 'تجهيز الامتحانات'), ('2/3', 'تفتيش'),
    ('2/4', 'غش'), ('2/5', 'نتائج دراسية'),
    ('3', 'أوامر ملكية'),
    ('4', 'أوقاف'), ('4/1', 'توزيع ريع الأوقاف'), ('4/2', 'مناقشة قرارات'),
    ('5', 'بعثات'), ('5/1', 'بعثات علمية'), ('5/2', 'بعثات الدعوة والإرشاد'),
    ('6', 'تعليم'), ('6/1', 'تطوير التعليم'), ('6/2', 'جداول دراسية'), ('6/3', 'المرحلة الابتدائية'),
    ('6/4', 'المرحلة الثانوية'), ('6/5', 'جامعة الأزهر'),
    ('7', 'شئون الطلاب'), ('7/1', 'إحصائية الطلاب'), ('7/2', 'طلاب مصريون'), ('7/3', 'طلاب وافدون'),
    ('7/4', 'طلاب مكفوفون'),
    ('8', 'شئون المعاهد'), ('8/1', 'إنشاء معاهد'), ('8/2', 'ترميم مباني'), ('8/3', 'تفتيش'),
    ('9', 'ميزانيات'),
    ('10', 'هيئة إدارية'), ('10/1', 'أجازات'), ('10/2', 'تعيينات'), ('10/3', 'ترقيات'), ('10/4', 'تقاعد'),
    ('10/5', 'انتداب'), ('10/6', 'استحقاقات مالية'), ('10/6/1', 'بدل جراية'), ('10/6/2', 'بدل سفر'),
    ('10/6/3', 'مرتبات'), ('10/6/4', 'علاوات'), ('10/6/5', 'مكافآت'),
]
CODES = {c for c, _ in SUBJECTS}


def q(v):
    if v is None:
        return 'NULL'
    v = re.sub(r'\s+', ' ', str(v)).strip()
    if v == '' or re.fullmatch(r'[-ـ\s]+', v):
        return 'NULL'
    return "'" + v.replace("'", "''") + "'"


def qi(v):
    v = (v or '').strip()
    return v if re.fullmatch(r'\d+', v) else 'NULL'


def years(*texts):
    ys = [int(y) for t in texts if t for y in re.findall(r'(?<!\d)(1[89]\d\d|20\d\d)(?!\d)', t)]
    return (min(ys), max(ys)) if ys else (None, None)


def expand(row):
    cells = []
    for c in row:
        cells.append(c['t'])
        cells.extend([None] * (c['span'] - 1))
    return cells


def nospace(s):
    return re.sub(r'\s+', '', s or '')


def seq_of(code):
    """Inventory sequence number: first number after the classification symbol."""
    m = re.search(r'ج\.ع\.أ/+[^\d/]*/*\s*(\d+)/', nospace(code))
    return m.group(1) if m else None


tables = [(i, o) for i, o in enumerate(doc) if o['type'] == 'table']

# ---------- master inventory of files ----------
files = []
for r in tables[1][1]['rows'][1:]:
    c = expand(r)
    if c[1] == 'رقم الملف' or not any((x or '').strip() for x in c[1:4]):
        continue
    y = years(c[6], c[7])
    files.append(dict(number=c[1], ref=c[2], title=c[3], color=c[4], size=c[5], start=c[6], end=c[7],
                      pages=c[8], lang=c[9], content=c[10], cond=c[11], notes=c[12], y=y))

# ---------- master inventory of registers ----------
regs = []
for r in tables[2][1]['rows'][1:]:
    c = expand(r)
    if c[1] == 'رقم السجل' or not any((x or '').strip() for x in c[1:4]):
        continue
    y = years(c[6], c[7])
    regs.append(dict(number=c[1], ref=c[2], title=c[3], unit=c[4], direction=c[5], start=c[6], end=c[7],
                     subjects=c[8], written=c[9], blank=c[10], lang=c[11], script=c[12], length=c[13],
                     width=c[14], cond=c[15], y=y))

# ---------- subject tables ----------
entries = []
current = None
for i, o in enumerate(doc):
    if i < 95:
        continue
    if o['type'] == 'p':
        m = re.match(r'^(\d+(?:/\d+)*)\s*[-ـ:]', o['t'])
        if m and m.group(1) in CODES:
            current = m.group(1)
        continue
    rows = o['rows']
    kind = 'REGISTER' if 'السجل' in rows[0][1]['t'] else 'FILE'
    for idx, r in enumerate(rows[1:], 1):
        c = [x['t'] for x in r]
        if c[1] in ('رقم الملف', 'رقم السجل') or not any(x.strip() for x in c[1:]):
            continue
        entries.append(dict(subject=current, kind=kind, serial=c[0], number=c[1], ref=c[2], title=c[3],
                            date=c[4], content=c[5], y=years(c[4])))


# ---------- link subject entries to inventory rows ----------
def build_index(items):
    by_ref = {nospace(x['ref']): k for k, x in enumerate(items)}
    by_num = collections.defaultdict(list)
    by_seq = {}
    for k, x in enumerate(items):
        by_num[nospace(x['number'])].append(k)
        s = seq_of(x['ref'])
        if s:
            by_seq[s] = k
    return by_ref, by_num, by_seq


idx = {'FILE': build_index(files), 'REGISTER': build_index(regs)}
master = {'FILE': files, 'REGISTER': regs}
stats = collections.Counter()
for e in entries:
    by_ref, by_num, by_seq = idx[e['kind']]
    num, seq = nospace(e['number']), seq_of(e['ref'])
    link = None
    items = master[e['kind']]
    r = by_ref.get(nospace(e['ref']))
    if r is not None and nospace(items[r]['number']) == num:
        link, how = r, 'ref+num'
    elif seq and seq in by_seq and nospace(items[by_seq[seq]]['number']) == num:
        link, how = by_seq[seq], 'seq+num'
    elif len(by_num.get(num, [])) == 1:
        link, how = by_num[num][0], 'num'
    elif r is not None:
        link, how = r, 'ref-only'
    else:
        how = 'none'
    e['link'] = link
    stats[(e['kind'], how)] += 1

print('files', len(files), 'registers', len(regs), 'entries', len(entries), file=sys.stderr)
print(dict(stats), file=sys.stderr)
print('entries without subject:', sum(1 for e in entries if not e['subject']), file=sys.stderr)

HDR = '-- Generated from "تصور قاعدة البيانات بعد التعديل.docx". Do not edit by hand.\n\n'

# V2 subjects
with open(os.path.join(out_dir, 'V2__seed_subjects.sql'), 'w', encoding='utf8') as f:
    f.write(HDR)
    for n, (code, name) in enumerate(SUBJECTS, 1):
        parent = code.rsplit('/', 1)[0] if '/' in code else None
        level = code.count('/') + 1
        ps = f"(SELECT id FROM subject WHERE code = {q(parent)})" if parent else 'NULL'
        f.write(f"INSERT INTO subject (code, name, parent_id, level, sort_order) VALUES ({q(code)}, {q(name)}, {ps}, {level}, {n});\n")

# V3 files
with open(os.path.join(out_dir, 'V3__seed_archive_files.sql'), 'w', encoding='utf8') as f:
    f.write(HDR)
    f.write('INSERT INTO archive_file (serial_no, file_number, reference_code, title, cover_color, file_size, '
            'start_date, end_date, year_from, year_to, sheet_count, language, subject_content, physical_condition, notes) VALUES\n')
    rows = []
    for n, x in enumerate(files, 1):
        y0, y1 = x['y']
        rows.append(f"({n}, {q(x['number'])}, {q(x['ref'])}, {q(x['title'])}, {q(x['color'])}, {q(x['size'])}, "
                    f"{q(x['start'])}, {q(x['end'])}, {y0 or 'NULL'}, {y1 or 'NULL'}, {qi(x['pages'])}, {q(x['lang'])}, "
                    f"{q(x['content'])}, {q(x['cond'])}, {q(x['notes'])})")
    f.write(',\n'.join(rows) + ';\n')

# V4 registers
with open(os.path.join(out_dir, 'V4__seed_archive_registers.sql'), 'w', encoding='utf8') as f:
    f.write(HDR)
    f.write('INSERT INTO archive_register (serial_no, register_number, reference_code, title, sub_unit_name, direction, '
            'start_date, end_date, year_from, year_to, subjects_text, written_sheets, blank_sheets, language, script_type, '
            'length_cm, width_cm, physical_condition) VALUES\n')
    rows = []
    for n, x in enumerate(regs, 1):
        y0, y1 = x['y']
        rows.append(f"({n}, {q(x['number'])}, {q(x['ref'])}, {q(x['title'])}, {q(x['unit'])}, {q(x['direction'])}, "
                    f"{q(x['start'])}, {q(x['end'])}, {y0 or 'NULL'}, {y1 or 'NULL'}, {q(x['subjects'])}, {qi(x['written'])}, "
                    f"{qi(x['blank'])}, {q(x['lang'])}, {q(x['script'])}, {qi(x['length'])}, {qi(x['width'])}, {q(x['cond'])})")
    f.write(',\n'.join(rows) + ';\n')

# V5 subject entries
with open(os.path.join(out_dir, 'V5__seed_subject_entries.sql'), 'w', encoding='utf8') as f:
    f.write(HDR)
    f.write('INSERT INTO subject_entry (subject_id, entry_type, serial_no, item_number, reference_code, title, date_text, '
            'year_from, year_to, subject_content, archive_file_id, archive_register_id) VALUES\n')
    rows = []
    per_subject = collections.Counter()
    for e in entries:
        per_subject[e['subject']] += 1
        serial = e['serial'].strip() if re.fullmatch(r'\s*\d+\s*', e['serial'] or '') else str(per_subject[e['subject']])
        y0, y1 = e['y']
        fid = rid = 'NULL'
        if e['link'] is not None:
            ref = f"(SELECT id FROM {'archive_file' if e['kind'] == 'FILE' else 'archive_register'} WHERE serial_no = {e['link'] + 1})"
            if e['kind'] == 'FILE':
                fid = ref
            else:
                rid = ref
        rows.append(f"((SELECT id FROM subject WHERE code = {q(e['subject'])}), '{e['kind']}', {serial}, {q(e['number'])}, "
                    f"{q(e['ref'])}, {q(e['title'])}, {q(e['date'])}, {y0 or 'NULL'}, {y1 or 'NULL'}, {q(e['content'])}, {fid}, {rid})")
    f.write(',\n'.join(rows) + ';\n')
