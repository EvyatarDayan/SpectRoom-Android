#!/usr/bin/env python3
"""Sync iOS Localizable.strings into Android values-*/strings.xml."""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IOS_ROOT = Path("/Users/evyatardayan/Desktop/My projects/swift/SpectRoom/SpectRoom")
ANDROID_RES = ROOT / "app/src/main/res"

LOCALES = {
    "en": None,
    "sv": "values-sv",
    "es": "values-es",
    "fr": "values-fr",
    "de": "values-de",
    "da": "values-da",
    "nb": "values-nb",
    "fi": "values-fi",
    "is": "values-is",
}

SPECIAL_IOS_TO_ANDROID = {
    "nav.messages": "tab_messages",
    "nav.matches": "tab_matches",
    "nav.profile": "tab_profile",
    "nav.settings": "tab_settings",
    "nav.discover": "tab_home",
    "settings.title": "settings_title",
    "settings.dark.mode": "settings_dark_mode",
    "settings.dark.mode.description": "settings_dark_mode_description",
    "settings.light.mode": "settings_light_mode",
    "settings.language.description": "settings_language_description",
    "settings.notifications.description": "settings_notifications_description",
    "settings.section.preferences": "settings_section_preferences",
    "settings.section.privacy.safety": "settings_section_privacy_safety",
    "settings.section.account.data": "settings_section_account_data",
    "settings.section.support.about": "settings_section_support_about",
    "settings.privacy.settings": "settings_privacy_settings",
    "settings.privacy.safety.description": "settings_privacy_safety_description",
    "settings.help.support": "settings_help",
    "settings.help.support.description": "settings_help_description",
    "settings.about.spectroom": "settings_about",
    "settings.about.description": "settings_about_description",
    "settings.sign.out": "settings_sign_out",
    "settings.authentication.signout.subtitle": "settings_sign_out_subtitle",
    "settings.delete.account.button": "settings_delete_account",
    "settings.delete.account.subtitle": "settings_delete_account_subtitle",
    "settings.blocked.users.button.subtitle": "settings_blocked_subtitle",
    "settings.blocked.users.button.title.count": "settings_blocked_users_count",
    "settings.reported.profiles.button.title.count": "settings_reported_profiles_count",
    "settings.reported.profiles.button.subtitle": "settings_reported_profiles_subtitle",
    "settings.export.messages": "settings_export_messages",
    "settings.export.messages.description": "settings_export_messages_description",
    "matches.title": "matches_title",
    "messages.title": "messages_title",
    "support.nav.title": "support_nav_title",
    "support.title": "support_title",
    "support.subtitle": "support_subtitle",
    "support.topic.label": "support_topic_label",
    "support.topic.safety": "support_topic_safety",
    "support.topic.bug": "support_topic_bug",
    "support.topic.feedback": "support_topic_feedback",
    "support.topic.account.freeze": "support_topic_account_freeze",
    "support.topic.account.ban": "support_topic_account_ban",
    "support.topic.account.deleted": "support_topic_account_deleted",
    "support.topic.other": "support_topic_other",
    "support.message.label": "support_message_label",
    "support.message.placeholder": "support_message_placeholder",
    "support.message.min.chars": "support_message_min_chars",
    "support.button.send": "support_button_send",
    "support.button.sent": "support_button_sent",
    "support.or": "support_or",
    "support.email.label": "support_email_label",
    "common.close": "common_close",
    "common.ok": "common_ok",
    "common.cancel": "settings_cancel",
    "common.done": "settings_done",
}


def ios_to_android_key(ios_key: str) -> str:
    if ios_key in SPECIAL_IOS_TO_ANDROID:
        return SPECIAL_IOS_TO_ANDROID[ios_key]
    return ios_key.replace(".", "_")


def parse_ios_strings(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    content = path.read_text(encoding="utf-8")
    pattern = re.compile(r'"([^"]+)"\s*=\s*"((?:\\.|[^"\\])*)"\s*;')
    result: dict[str, str] = {}
    for key, value in pattern.findall(content):
        value = (
            value.replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
        )
        result[key] = value
    return result


def convert_placeholders(value: str) -> str:
    value = re.sub(r"%(\d+)\$@", r"%\1$s", value)
    value = re.sub(r"%@", "%1$s", value)
    value = re.sub(r"%(\d+)\$d", r"%\1$d", value)
    value = value.replace("&", "&amp;")
    value = value.replace("<", "&lt;")
    return value


def fix_apostrophes_in_strings_xml(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    def fix(match: re.Match[str]) -> str:
        open_tag, body, close = match.group(1), match.group(2), match.group(3)
        body = body.replace("\\'", "'")
        body = re.sub(r"(?<!\\)'", r"\\'", body)
        return f"{open_tag}{body}{close}"

    path.write_text(
        re.sub(r"(<string[^>]*>)(.*?)(</string>)", fix, text, flags=re.DOTALL),
        encoding="utf-8",
    )


def load_android_names(values_dir: Path) -> set[str]:
    strings_file = values_dir / "strings.xml"
    if not strings_file.exists():
        return set()
    tree = ET.parse(strings_file)
    return {elem.attrib["name"] for elem in tree.getroot().findall("string")}


def append_missing_keys(values_dir: Path, translations: dict[str, str], android_names: set[str]) -> int:
    strings_file = values_dir / "strings.xml"
    if not strings_file.exists() or values_dir.name == "values":
        return 0
    base_file = ANDROID_RES / "values" / "strings.xml"
    base_tree = ET.parse(base_file)
    tree = ET.parse(strings_file)
    root = tree.getroot()
    existing = {elem.attrib["name"] for elem in root.findall("string")}
    added = 0
    for base_elem in base_tree.getroot().findall("string"):
        name = base_elem.attrib["name"]
        if name in existing:
            continue
        ios_key = next(
            (ios for ios, android in SPECIAL_IOS_TO_ANDROID.items() if android == name),
            name.replace("_", "."),
        )
        value = translations.get(ios_key, base_elem.text or "")
        value = convert_placeholders(value)
        elem = ET.SubElement(root, "string", {"name": name})
        elem.text = value
        added += 1
    if added:
        ET.indent(tree, space="    ")
        tree.write(strings_file, encoding="utf-8", xml_declaration=True)
    return added


def update_android_strings(values_dir: Path, translations: dict[str, str], android_names: set[str]) -> int:
    strings_file = values_dir / "strings.xml"
    if not strings_file.exists():
        return 0

    text = strings_file.read_text(encoding="utf-8")
    updated = 0
    for ios_key, ios_value in translations.items():
        android_key = ios_to_android_key(ios_key)
        if android_key not in android_names:
            continue
        android_value = convert_placeholders(ios_value)
        pattern = re.compile(
            rf'(<string\s+name="{re.escape(android_key)}"[^>]*>)(.*?)(</string>)',
            re.DOTALL,
        )
        if not pattern.search(text):
            continue
        match = pattern.search(text)
        if not match:
            continue
        text = text[:match.start()] + f"{match.group(1)}{android_value}{match.group(3)}" + text[match.end():]
        updated += 1
    strings_file.write_text(text, encoding="utf-8")
    return updated


def main() -> None:
    base_android_names = load_android_names(ANDROID_RES / "values")
    total = 0
    for ios_locale, android_dir in LOCALES.items():
        ios_path = IOS_ROOT / f"{ios_locale}.lproj" / "Localizable.strings"
        ios_strings = parse_ios_strings(ios_path)
        if not ios_strings:
            print(f"skip {ios_locale}: no iOS strings")
            continue
        target_dir = ANDROID_RES / (android_dir or "values")
        count = update_android_strings(target_dir, ios_strings, base_android_names)
        added = append_missing_keys(target_dir, ios_strings, base_android_names)
        fix_apostrophes_in_strings_xml(target_dir / "strings.xml")
        print(f"{ios_locale}: updated {count}, added {added} in {target_dir.name}")
        total += count
    print(f"total updates: {total}")


if __name__ == "__main__":
    main()
