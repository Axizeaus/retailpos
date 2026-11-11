ALTER TABLE products
    ADD COLUMN barcode VARCHAR(50);

ALTER TABLE products
    ADD CONSTRAINT uk_products_barcode UNIQUE (barcode);