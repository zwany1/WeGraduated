USE thesis_format_test;

INSERT INTO t_format_template (user_id, name, heading_patterns, generate_toc, generate_abstract, is_public, recommended, rating_avg, rating_count, create_time, update_time)
VALUES (1, '测试模板', '{"heading1":"^第[一二三四五六七八九十百]+章","heading2":"^ZZZ","heading3":"^YYY"}', 0, 0, 0, 0, 0.0, 0, NOW(), NOW());
