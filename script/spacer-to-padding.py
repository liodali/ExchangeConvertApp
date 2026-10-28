#!/usr/bin/env python3
"""Convert fixed-size layout Spacers to padding modifiers (Flutter SizedBox→padding style).

Minimal-touch transform — every line outside the touched lines stays byte-identical:

    Spacer(Modifier.height(12.dp))        Text(
    Text(                                     modifier = Modifier.padding(top = 12.dp),
        text = "hi",            ->            text = "hi",
        ...                                     ...
    )                                     )

  - element WITHOUT a modifier arg  -> insert `modifier = Modifier.padding(top = X.dp),`
    right after the call's open line (safe: padding sits outside backgrounds/click
    areas — exactly Spacer's geometry). Only when ALL args are named (inserting a
    named arg into a positional call is a compile error).
  - element WITH `modifier = Modifier...` -> chain `.padding(top = X.dp)` right after
    `Modifier.` (found anywhere in the call, including on the open line).
  - trailing Spacer before '}' -> same, but bottom/end padding on the PREVIOUS element.

Skips (left untouched): GlanceModifier widget code, Spacer(Modifier.weight(...)),
dynamic sizes (non-literal dp), Spacer followed by another Spacer, positional-arg
calls, calls whose modifier isn't a `Modifier.` chain, padding-first chains.

Usage:  spacer-to-padding.py --dry [paths...]   # report only
        spacer-to-padding.py [paths...]         # apply (default: shared commonMain + app)
"""
import re
import sys
import pathlib

SPACER_RE = re.compile(
    r'^(\s*)Spacer\((?:modifier\s*=\s*)?Modifier\.(height|width)\((\d+\.dp)\)\s*\)\s*,?\s*$')
ELEM_CALL_RE = re.compile(r'^(\s*)([A-Z][A-Za-z0-9_.]*)\($')
NAMED_ARG_RE = re.compile(r'^\s*[a-zA-Z_]\w*\s*=')


def call_close(lines, start):
    """Index of the line where the paren opened on `start` closes (depth back to 0)."""
    depth = 0
    for i in range(start, len(lines)):
        for ch in lines[i]:
            if ch == '(':
                depth += 1
            elif ch == ')':
                depth -= 1
                if depth == 0:
                    return i
    return None


def split_args(text):
    """Split call-argument text on top-level commas."""
    args, depth, cur = [], 0, ''
    for ch in text:
        if ch in '([{':
            depth += 1
        elif ch in ')]}':
            depth -= 1
        if ch == ',' and depth == 0:
            args.append(cur)
            cur = ''
        else:
            cur += ch
    if cur.strip():
        args.append(cur)
    return args


def plan_call(lines, open_idx, close_idx, pad):
    """Decide how to add `Modifier.pad` to the call. Returns ('chain', line_idx, new_line)
    | ('insert',) | None (= skip). Non-destructive."""
    first = lines[open_idx]
    pos = first.rfind('(')
    body_lines = [first[pos + 1:]] + lines[open_idx + 1:close_idx]
    args = split_args('\n'.join(body_lines))
    args = [a for a in args if a.strip()]

    mod_arg = None
    for a in args:
        if NAMED_ARG_RE.match(a) is None:
            return None  # positional arg present — can't insert/modify named args safely
        if re.match(r'^\s*modifier\s*=', a):
            mod_arg = a
    if mod_arg is not None:
        if not re.search(r'\bmodifier\s*=\s*Modifier\.', mod_arg):
            return None  # modifier is a variable / different receiver — can't order safely
        first_seg = re.sub(r'^\s*modifier\s*=\s*Modifier\.', '', mod_arg).split('(')[0].strip()
        if first_seg == 'padding' or first_seg == '':
            return None  # padding already first / bare Modifier
        # find the physical line carrying `modifier = ... Modifier.`
        for i in range(open_idx, close_idx + 1):
            if re.search(r'\bmodifier\s*=\s*Modifier\.', lines[i]):
                return ('chain', i, re.sub(r'\bmodifier\s*=\s*Modifier\.',
                                           f'modifier = Modifier.{pad}.', lines[i], count=1))
        return None
    return ('insert',)


def convert(lines, spacer_idx, target_idx, axis, size, before_open=True):
    close = call_close(lines, target_idx)
    if close is None:
        return None
    pos = ('top' if axis == 'height' else 'start') if before_open else \
          ('bottom' if axis == 'height' else 'end')
    pad = f'padding({pos} = {size})'
    plan = plan_call(lines, target_idx, close, pad)
    if plan is None:
        return None
    open_indent = re.match(r'\s*', lines[target_idx]).group(0)
    new_lines = lines[:]
    if plan[0] == 'insert':
        new_lines[target_idx + 1:target_idx + 1] = [f'{open_indent}    modifier = Modifier.{pad},']
        desc = 'inserted modifier arg'
    else:
        _, li, newline = plan
        new_lines[li] = newline
        desc = 'chained into modifier'
    del new_lines[spacer_idx:target_idx]
    elem = lines[target_idx].strip().split('(')[0]
    return (new_lines, f'Spacer {axis}({size}) → {elem}.{pad} ({desc})')


def transform_file(path, dry=False):
    src = path.read_text()
    if 'Spacer(GlanceModifier' in src:
        return []  # widget code untouched
    lines = src.split('\n')
    changes = []
    i = 0
    while i < len(lines):
        m = SPACER_RE.match(lines[i])
        if not m:
            i += 1
            continue
        axis, size = m.group(2), m.group(3)

        # peek next non-blank / non-comment line
        j = i + 1
        while j < len(lines) and (not lines[j].strip() or lines[j].strip().startswith('//')):
            j += 1
        nxt = lines[j].strip() if j < len(lines) else ''

        if nxt.startswith('Spacer'):  # consecutive spacers — leave alone
            i += 1
            continue

        if nxt and not nxt.startswith('}') and ELEM_CALL_RE.match(lines[j]):
            r = convert(lines, i, j, axis, size, before_open=True)
        elif i > 0 and ELEM_CALL_RE.match(lines[i - 1]):
            # trailing spacer (next is blank/'}') → bottom/end padding on previous element
            r = convert(lines, i, i - 1, axis, size, before_open=False)
        else:
            r = None

        if r is None:
            i += 1
            continue
        lines, desc = r[0], r[1]
        changes.append((i + 1, desc))
        continue

    if changes and not dry:
        path.write_text('\n'.join(lines))
    return changes


def main():
    argv = sys.argv[1:]
    dry = '--dry' in argv
    targets = [a for a in argv if a != '--dry'] or ['shared/src/commonMain/kotlin', 'app/src/main/java']
    total = files = 0
    for t in targets:
        p = pathlib.Path(t)
        for f in (p.rglob('*.kt') if p.is_dir() else [p]):
            r = transform_file(f, dry)
            if r:
                files += 1
                total += len(r)
                print(f'{f}:')
                for ln, desc in r[:5]:
                    print(f'  L{ln}: {desc}')
                if len(r) > 5:
                    print(f'  … +{len(r) - 5} more')
    print(f'\n{"[DRY] would convert" if dry else "converted"}: {total} spacers in {files} files')


if __name__ == '__main__':
    main()
