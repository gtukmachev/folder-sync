package tga.folder_sync.files

import java.lang.RuntimeException

/**
 * Created by grigory@clearscale.net on 2/23/2019.
 */
class FolderDoNotExistsException(fileName: String)
       : RuntimeException("the file or folder '$fileName' do not exist!")

class NotAFolderException(fileName: String)
       : RuntimeException("The file-system object '$fileName' is not a folder!")
