package com.jobradar.externaljobs;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Element;

final class IndiaJobSources {
    private static final Pattern INDIA = Pattern.compile(
            "\\b(india|hyderabad|secunderabad|telangana|bengaluru|bangalore|chennai|pune|mumbai|navi mumbai|delhi|ncr|gurgaon|gurugram|noida|kolkata|ahmedabad|kochi|cochin|thiruvananthapuram|trivandrum|coimbatore|jaipur|indore|lucknow|bhubaneswar|chandigarh|vadodara|surat|nagpur|mysuru|mangalore|visakhapatnam|vizag|vijayawada|patna|goa|kerala|maharashtra|karnataka|tamil nadu|uttar pradesh|west bengal|odisha|rajasthan|gujarat|andhra pradesh)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_ONLY = Pattern.compile(
            "\\b(us|u\\.s\\.|usa|uk|u\\.k\\.|canada|europe|australia|united states|united kingdom)[ -]+(based|only|residents?)\\b|\\bonly (in|within) (the )?(us|usa|uk|europe|canada)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TECH = Pattern.compile(
            "\\b(software|developer|programmer|information technology|it support|technical support|sre|site reliability|data scientist|data analyst|machine learning|systems engineer|solutions architect|network engineer|platform engineer|ai engineer)\\b",
            Pattern.CASE_INSENSITIVE);

    private final RestClient client;

    IndiaJobSources(RestClient client) {
        this.client = client;
    }

    List<ExternalJobDto> greenhouse(String slug, String company, Map<String, Integer> categories, Instant now) {
        JsonNode rows = array(json("https://boards-api.greenhouse.io/v1/boards/" + slug + "/jobs"), "jobs");
        var jobs = new ArrayList<ExternalJobDto>();
        for (JsonNode row : rows) {
            String location = indiaLocation(Stream.of(row.path("location").path("name").asText()));
            String url = row.path("absolute_url").asText();
            add(jobs, job("Greenhouse", company, row.path("title").asText(), null, List.of(),
                    location, null, null, url, url, date(row.path("first_published").asText()), null, now, categories));
        }
        return jobs;
    }

    List<ExternalJobDto> lever(String slug, String company, Map<String, Integer> categories, Instant now) {
        var jobs = new ArrayList<ExternalJobDto>();
        for (int offset = 0; offset < 1000; offset += 100) {
            JsonNode rows = json("https://api.lever.co/v0/postings/" + slug + "?mode=json&limit=100&skip=" + offset);
            if (!rows.isArray()) throw new RestClientException("Invalid Lever response");
            for (JsonNode row : rows) {
                JsonNode info = row.path("categories");
                String location = indiaLocation(Stream.concat(Stream.of(info.path("location").asText()),
                        strings(info.path("allLocations")).stream()));
                add(jobs, job("Lever", company, row.path("text").asText(), info.path("team").asText(null),
                        List.of(), location, info.path("commitment").asText(null), row.path("workplaceType").asText(),
                        row.path("hostedUrl").asText(), row.path("applyUrl").asText(),
                        timestamp(row.path("createdAt"), true), null, now, categories));
            }
            if (rows.size() < 100) break;
        }
        return jobs;
    }

    List<ExternalJobDto> ashby(String slug, String company, Map<String, Integer> categories, Instant now) {
        JsonNode rows = array(json("https://api.ashbyhq.com/posting-api/job-board/" + slug), "jobs");
        var jobs = new ArrayList<ExternalJobDto>();
        for (JsonNode row : rows) {
            if (!row.path("isListed").asBoolean(true)) continue;
            var places = new ArrayList<String>();
            places.add(row.path("location").asText());
            for (JsonNode secondary : row.path("secondaryLocations")) places.add(secondary.path("location").asText());
            add(jobs, job("Ashby", company, row.path("title").asText(), row.path("team").asText(null),
                    List.of(), indiaLocation(places.stream()), row.path("employmentType").asText(null),
                    row.path("workplaceType").asText(), row.path("jobUrl").asText(), row.path("applyUrl").asText(),
                    date(row.path("publishedAt").asText()), null, now, categories));
        }
        return jobs;
    }

    List<ExternalJobDto> smartRecruiters(String board, String company, Map<String, Integer> categories, Instant now) {
        var jobs = new ArrayList<ExternalJobDto>();
        int offset = 0;
        while (true) {
            JsonNode response = json("https://api.smartrecruiters.com/v1/companies/" + board
                    + "/postings?country=in&limit=100&offset=" + offset);
            JsonNode rows = array(response, "content");
            for (JsonNode row : rows) {
                JsonNode place = row.path("location");
                String id = row.path("id").asText();
                if (!"in".equalsIgnoreCase(place.path("country").asText()) || !id.matches("[0-9]+")) continue;
                String url = "https://jobs.smartrecruiters.com/" + board + "/" + id;
                String location = Stream.of(place.path("city").asText(), place.path("region").asText(), "India")
                        .map(String::trim).filter(part -> !part.isBlank()).distinct()
                        .collect(java.util.stream.Collectors.joining(", "));
                String workplace = place.path("remote").asBoolean() ? "remote"
                        : place.path("hybrid").asBoolean() ? "hybrid" : null;
                add(jobs, job("SmartRecruiters", company, row.path("name").asText(),
                        row.path("department").path("label").asText(null), List.of(), location,
                        row.path("typeOfEmployment").path("label").asText(null), workplace, url, url,
                        date(row.path("releasedDate").asText()), null, now, categories));
            }
            offset += rows.size();
            if (rows.isEmpty() || offset >= response.path("totalFound").asInt(offset)) return jobs;
        }
    }

    List<ExternalJobDto> himalayas(Map<String, Integer> categories, Instant now) {
        var jobs = new ArrayList<ExternalJobDto>();
        for (int page = 1; page <= 5; page++) {
            JsonNode rows;
            try {
                rows = array(json("https://himalayas.app/jobs/api/search?country=IN&exclude_worldwide=true&sort=recent&page=" + page), "jobs");
            } catch (RestClientException ex) {
                if (page == 1) throw ex;
                break;
            }
            for (JsonNode row : rows) {
                if (!strings(row.path("locationRestrictions")).contains("India")) continue;
                JsonNode zones = row.path("timezoneRestrictions");
                if (zones.isArray() && !zones.isEmpty() && zones.valueStream().noneMatch(zone -> zone.asDouble() == 5.5)) continue;
                Instant expiry = timestamp(row.path("expiryDate"), false);
                if (expiry != null && !expiry.isAfter(now)) continue;
                String application = row.path("applicationLink").asText();
                if (!"himalayas.app".equals(host(application))) continue;
                List<String> tags = strings(row.path("categories")).stream().map(value -> value.replace('-', ' ')).limit(6).toList();
                add(jobs, job("Himalayas", row.path("companyName").asText(), row.path("title").asText(),
                        row.path("excerpt").asText(null), tags, "Remote (India)", row.path("employmentType").asText(null),
                        "REMOTE", row.path("guid").asText(), application, timestamp(row.path("pubDate"), false),
                        expiry == null ? null : LocalDate.ofInstant(expiry, ZoneOffset.UTC), now, categories));
            }
            if (rows.size() < 20) break;
        }
        return jobs;
    }

    List<ExternalJobDto> remoteRss(Map<String, Integer> categories, Instant now) {
        byte[] xml = client.get().uri("https://weworkremotely.com/remote-jobs.rss").retrieve().body(byte[].class);
        if (xml == null) throw new RestClientException("Empty We Work Remotely RSS feed");
        try {
            var parser = DocumentBuilderFactory.newInstance();
            parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            parser.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var items = parser.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getElementsByTagName("item");
            var jobs = new ArrayList<ExternalJobDto>();
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String region = value(item, "region");
                String country = value(item, "country");
                String heading = value(item, "title");
                boolean explicitlyIndia = "IN".equalsIgnoreCase(country) || INDIA.matcher(country + " " + region).find();
                boolean worldwide = country.isBlank() && region.equalsIgnoreCase("Anywhere in the World");
                if ((!explicitlyIndia && !worldwide) || FOREIGN_ONLY.matcher(heading).find()) continue;
                String[] title = heading.split(":\\s*", 2);
                String company = title.length == 2 ? title[0] : "We Work Remotely";
                String url = value(item, "link");
                if (!"weworkremotely.com".equals(host(url))) continue;
                List<String> skills = Arrays.stream(value(item, "skills").split(","))
                        .map(String::trim).filter(skill -> !skill.isBlank()).toList();
                add(jobs, job("We Work Remotely", company, title[title.length - 1], value(item, "category"), skills,
                        worldwide ? "Remote (worldwide)" : "Remote (India)", value(item, "type"), "REMOTE", url, url,
                        rssDate(value(item, "pubDate")), null, now, categories));
            }
            return jobs;
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException ex) {
            throw new RestClientException("Invalid We Work Remotely RSS feed", ex);
        }
    }

    private static ExternalJobDto job(String source, String company, String title, String summary, List<String> skills,
            String location, String employment, String workplace, String url, String apply, Instant posted,
            LocalDate expires, Instant now, Map<String, Integer> categories) {
        if (title.isBlank() || company.isBlank() || location.isBlank() || host(url) == null) return null;
        String category = ExternalJobRoleMatcher.category(title, skills, summary, TECH.matcher(title).find());
        Integer categoryId = category == null ? null : categories.get(category);
        String mode = switch (workplace == null ? "" : workplace.toLowerCase(Locale.ROOT)) {
            case "remote" -> "REMOTE";
            case "hybrid" -> "HYBRID";
            case "onsite", "on-site", "on_site" -> "ON_SITE";
            default -> location.toLowerCase(Locale.ROOT).contains("remote") ? "REMOTE" : null;
        };
        return new ExternalJobDto(canonicalUrl(url), title, company, null, summary, location, null, "India",
                employment, null, null, skills, mode, url, host(apply) == null ? url : apply, source,
                posted, now, expires, true, categoryId, null, null, null, null);
    }

    static String canonicalUrl(String url) {
        URI uri = URI.create(url);
        String path = uri.getRawPath().replaceFirst("/+$", "");
        String query = uri.getRawQuery();
        if (query != null) {
            query = Arrays.stream(query.split("&")).filter(part -> !part.startsWith("utm_"))
                    .sorted().collect(java.util.stream.Collectors.joining("&"));
        }
        return uri.getHost().toLowerCase(Locale.ROOT) + path + (query == null || query.isEmpty() ? "" : "?" + query);
    }

    static String indiaLocation(Stream<String> locations) {
        return locations.flatMap(place -> Arrays.stream(place.split("[;|]|\\s+/\\s+")))
                .map(String::trim).filter(part -> INDIA.matcher(part).find()).distinct()
                .collect(java.util.stream.Collectors.joining(" / "));
    }

    private static String host(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null ? uri.getHost() : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void add(List<ExternalJobDto> jobs, ExternalJobDto job) {
        if (job != null) jobs.add(job);
    }

    private JsonNode json(String url) {
        JsonNode result = client.get().uri(url).retrieve().body(JsonNode.class);
        if (result == null) throw new RestClientException("Empty careers response");
        return result;
    }

    private static JsonNode array(JsonNode node, String key) {
        JsonNode rows = node.path(key);
        if (!rows.isArray()) throw new RestClientException("Invalid careers list");
        return rows;
    }

    private static List<String> strings(JsonNode values) {
        return values.isArray() ? values.valueStream().filter(JsonNode::isTextual).map(JsonNode::asText).toList() : List.of();
    }

    private static Instant timestamp(JsonNode value, boolean millis) {
        if (!value.isIntegralNumber() || value.asLong() <= 0) return null;
        return millis ? Instant.ofEpochMilli(value.asLong()) : Instant.ofEpochSecond(value.asLong());
    }

    private static Instant date(String value) {
        try { return Instant.parse(value); }
        catch (DateTimeParseException ex) { return null; }
    }

    private static Instant rssDate(String value) {
        try { return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
        catch (DateTimeParseException ex) { return null; }
    }

    private static String value(Element element, String field) {
        var nodes = element.getElementsByTagName(field);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
