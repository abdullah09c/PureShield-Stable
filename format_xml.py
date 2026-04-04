import xml.dom.minidom
dom = xml.dom.minidom.parse('app/window_dump.xml')
with open('app/dump_formatted.xml', 'w') as f:
    f.write(dom.toprettyxml())
