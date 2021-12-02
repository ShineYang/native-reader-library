package com.shawnyang.jpreader_lib

/**
 * @author ShineYang
 * @date 2021/9/15
 * description:
 */
enum class MessageType(val methodName: String) {
    OpenBook("open_book"),
    GetBookList("get_book_list"),
    FilePicker("open_file_picker"),
    RemoveFromLibrary("remove_from_library"),
    FlutterComponentInitCompleted("flutter_component_init_completed"),
}