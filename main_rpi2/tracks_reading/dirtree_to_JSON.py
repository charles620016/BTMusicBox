# coding=utf-8
import os
import eyed3

def path_to_dict(path):
	global count
	global filedict

	d = {'name': os.path.basename(path)}
	d['id'] = count
	filedict.update({count: path})
	count += 1
	if os.path.isdir(path):
		d['type'] = 'folder'
		d['children'] = [path_to_dict(os.path.join(path,x)) for x in os.listdir(path)]
	else:
		d['type'] = 'file'
		# extract the file extension
		format = os.path.splitext(path)[-1];
		if format =='.mp3':
			audiofile = eyed3.load(path)
			d['format'] = format
			d['artist'] = audiofile.tag.artist
			d['album'] = audiofile.tag.album
			d['playtime'] = audiofile.info.time_secs
		else:
			d['format'] = 'notaudio'
	return d


if __name__ == '__main__':
	import io
	import json
	import sys
	import csv

	try:
		path = sys.argv[1]
	except IndexError:
		path = '.'

	count = 0
	filedict = {}

	# create directory-tree json file
	with io.open('/home/pi/IODD/data/tracks.json', 'w', encoding='utf-8') as outfile:
		outfile.write(unicode(json.dumps(path_to_dict(path)))) # indent=0
		outfile.close()

	# create a dict about the directory-tree
	dictfile = csv.writer(open("/home/pi/IODD/data/filedict.csv", "w"))
	for key, val in filedict.items():
		dictfile.writerow([key, val])
