import os;

class build_pkg (dict):
    def __init__(self, name, version):
        self.update({
            "name"          : name,
            "version"       : version,
            "dirs"          : {},
            "files"         : {},
            "links"         : {}
        })
    
    # add directories recursively, kind of like "mkdir -p"
    def mkdirs(self,path,attributes={}):
        head = ""
        for i in path.split('/'):
            if len(i)==0 :
                continue
            if len(head) > 0 :
                head += '/'
            head += i
            if head not in self["dirs"]:
                # print 'Adding %s' % head
                self["dirs"][head] = attributes

    # add a file, and create parent directories if necessary
    def addfile(self,fullpath,attributes={}):
        self.mkdirs(fullpath[0:fullpath.rindex('/')])
        self["files"][fullpath] = attributes

    def addlink(self,fullpath,target):
        self["links"][fullpath] = target

    # recursively add files and sub-directories in src, to be installed into the 'target' dir
    def addRecursively(self,target,src,filter=None):
        for root, dirs, files in os.walk(src):
            reldir = root[len(src)+1:]
            for name in files:
                if filter==None or filter(name):
                    self.addfile(os.path.join(target,reldir,name), {"file":os.path.join(root,name)})
            for name in dirs:
                if filter==None or filter(name):
                    self.mkdirs(os.path.join(target,reldir,name))

