USE thesis_format_test;

INSERT INTO t_paper_file (user_id, original_name, stored_path, file_size, create_time)
VALUES (1, 'test-paper.docx', 'upload/20260829/7c353a464a2941a1ad8aaa801e12bf7a.docx', 120000, NOW());

-- 故意让 heading2/heading3 正则永远不匹配, 触发"疑似未匹配标题"收集
INSERT INTO t_format_template (user_id, name, heading_patterns, generate_toc, generate_abstract, is_public, recommended, download_count, rating_avg, rating_count, create_time, update_time)
VALUES (1, '测试模板', '{"heading1":"^第[一二三四五六七八九十百]+章","heading2":"^ZZZ","heading3":"^YYY"}', 0, 0, 0, 0, 0, 0.0, 0, NOW(), NOW());
