import struct, sys

def parse(b):
    tp, hsz, size = struct.unpack_from('<HHI', b, 0)
    assert tp == 0x0003, hex(tp)
    off = hsz
    chunks = []
    strings = None
    while off < size:
        ctype, chsz, csize = struct.unpack_from('<HHI', b, off)
        chunks.append((ctype, off, chsz, csize))
        if ctype == 0x0001:
            strings = parse_pool(b, off, chsz, csize)
        off += csize
    return chunks, strings

def parse_pool(b, off, hsz, size):
    cnt, style_cnt, flags, str_start, style_start = struct.unpack_from('<IIIII', b, off+8)
    utf8 = bool(flags & 0x100)
    offs = struct.unpack_from('<%dI' % cnt, b, off + hsz)
    data_base = off + str_start
    out = []
    for o in offs:
        p = data_base + o
        if utf8:
            # utf16 length then utf8 length (varint)
            n1 = b[p]; p += 1
            if n1 & 0x80: p += 1
            n2 = b[p]; p += 1
            if n2 & 0x80:
                n2 = ((n2 & 0x7f) << 8) | b[p]; p += 1
            out.append(b[p:p+n2].decode('utf-8', 'replace'))
        else:
            n = struct.unpack_from('<H', b, p)[0]; p += 2
            out.append(b[p:p+n*2].decode('utf-16-le', 'replace'))
    return out

def walk(b):
    """yield (element_name, node_off, [(attr_name, attr_off, rawindex, dtype, data)])"""
    chunks, strings = parse(b)
    for ctype, off, hsz, csize in chunks:
        if ctype != 0x0102:
            continue
        ns_i, name_i = struct.unpack_from('<II', b, off+16)
        attr_start, attr_size, attr_count = struct.unpack_from('<HHH', b, off+16+8)
        base = off + 16 + attr_start
        attrs = []
        for i in range(attr_count):
            a = base + i*attr_size
            ans, aname, raw = struct.unpack_from('<III', b, a)
            vsz, res0, dtype, data = struct.unpack_from('<HBBI', b, a+12)
            attrs.append((strings[aname] if aname < len(strings) else '?', a, raw, dtype, data, ans))
        yield strings[name_i], off, attrs

def dump(path):
    b = open(path,'rb').read()
    for name, off, attrs in walk(b):
        print('ELEM', name, hex(off))
        for a in attrs:
            s = a[5]
            print('   ', (a[0]), 'dtype=%#x' % a[3], 'data=%#x' % a[4], ('raw=%d' % a[2]) if a[2] != 0xffffffff else '')

if __name__ == '__main__':
    dump(sys.argv[1])
