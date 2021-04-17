import config from "@/utils/config";

export function getLocalName(s: string) {
    let list = s.split(/[/#]/)
    return list[list.length - 1]
}
