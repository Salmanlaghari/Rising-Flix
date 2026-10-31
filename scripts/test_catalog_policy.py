import json
import unittest
from pathlib import Path

from build_content import ROOT, resolve_moviebox_page, sanitize_moviebox_item
from verify_licensed_catalog import EXPECTED, validate_catalog


class LicensedCatalogTests(unittest.TestCase):
    def test_checked_in_catalog_matches_reviewed_license_and_measured_runtime(self):
        catalog = json.loads((ROOT / "licensed_content.json").read_text(encoding="utf-8"))
        item = validate_catalog(catalog)[0]
        self.assertEqual("CC BY-SA 3.0", item["licenseName"])
        self.assertEqual(5586, item["actualDurationSeconds"])
        self.assertEqual(EXPECTED["videoUrl"], item["videoUrl"])

    def test_title_metadata_cannot_make_a_moviebox_stream_licensed(self):
        spoof = dict(EXPECTED)
        spoof["videoUrl"] = "https://macdn.aoneroom.com/media/claimed-full.mp4"
        with self.assertRaises(ValueError):
            validate_catalog({"featured": spoof, "categories": [{"name": "Movies", "items": [spoof]}]})

    def test_moviebox_legacy_detail_url_is_converted_to_current_page_route(self):
        self.assertEqual(
            "https://themoviebox.xyz/detail/black-panther-SllZTI6MG46",
            resolve_moviebox_page("https://moviebox.pk/moviedetail/black-panther-SllZTI6MG46"),
        )

    def test_stream_url_is_discarded_and_only_existing_detail_page_is_kept(self):
        row = sanitize_moviebox_item({
            "id": "mov_black_panther",
            "title": "Black Panther",
            "videoUrl": "https://macdn.aoneroom.com/unlicensed.mp4",
            "streaming_url": "https://macdn.aoneroom.com/unlicensed.mp4",
            "detailUrl": "https://moviebox.pk/moviedetail/black-panther-SllZTI6MG46",
            "duration": "2 h 14 m",
            "actualDurationSeconds": 138,
        })
        self.assertEqual("https://themoviebox.xyz/detail/black-panther-SllZTI6MG46", row["videoUrl"])
        self.assertEqual(row["videoUrl"], row["sourcePageUrl"])
        self.assertEqual("details_only", row["contentType"])
        self.assertIsNone(row["actualDurationSeconds"])
        self.assertEqual("Runtime unverified", row["duration"])
        self.assertNotIn("streaming_url", row)

    def test_missing_detail_page_never_falls_back_to_direct_stream(self):
        row = sanitize_moviebox_item({
            "id": "mov_no_page",
            "title": "No page",
            "videoUrl": "https://cdn.example.org/file.mp4",
        })
        self.assertEqual("", row["videoUrl"])
        self.assertIsNone(row["sourcePageUrl"])

    def test_generated_link_catalog_contains_no_moviebox_media_urls(self):
        for relative in (
            "content.json", "trending.json", "popular_dramas.json", "search.json",
            "data/moviebox_content.json", "data/themoviebox_content.json",
            "app/src/main/assets/moviebox_links.json", "app/src/main/assets/themoviebox_content.json",
        ):
            path = ROOT / relative
            if not path.exists():
                continue
            payload = json.loads(path.read_text(encoding="utf-8"))
            text = json.dumps(payload)
            self.assertNotIn("macdn.aoneroom.com", text, relative)
            self.assertNotIn("streaming_url", text.lower(), relative)

    def test_all_original_moviebox_category_rows_remain_visible(self):
        catalog = json.loads((ROOT / "app/src/main/assets/moviebox_links.json").read_text(encoding="utf-8"))
        self.assertEqual(8, len(catalog["categories"]))
        self.assertEqual(446, sum(len(category["items"]) for category in catalog["categories"]))
        self.assertTrue(all(item["contentType"] == "details_only" for category in catalog["categories"] for item in category["items"]))
        self.assertTrue(all(item["category"] == category["name"] for category in catalog["categories"] for item in category["items"]))


if __name__ == "__main__":
    unittest.main()
