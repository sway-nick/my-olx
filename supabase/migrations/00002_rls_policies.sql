-- ========================================================
-- 00002_rls_policies.sql: Row Level Security (RLS)
-- Mandatory Security Layer as defined in Section 27 of ТЗ
-- ========================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.intents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.external_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.external_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.analytics_events ENABLE ROW LEVEL SECURITY;

-- 1. Profiles
CREATE POLICY "Public profiles are viewable by everyone"
    ON public.profiles FOR SELECT
    USING (status = 'ACTIVE');

CREATE POLICY "Users can insert their own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- 2. Categories
CREATE POLICY "Categories are readable by everyone"
    ON public.categories FOR SELECT
    USING (is_active = TRUE);

-- 3. Intents (DEMAND / Private Requests)
CREATE POLICY "Users can manage their own intents"
    ON public.intents FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 4. Native Listings
CREATE POLICY "Active listings are viewable by everyone"
    ON public.listings FOR SELECT
    USING (status = 'ACTIVE');

CREATE POLICY "Users can insert their own listings"
    ON public.listings FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own listings"
    ON public.listings FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own listings"
    ON public.listings FOR DELETE
    USING (auth.uid() = user_id);

-- 5. External Sources & Listings
CREATE POLICY "External sources readable by everyone"
    ON public.external_sources FOR SELECT
    USING (is_active = TRUE);

CREATE POLICY "Fresh external listings are viewable by everyone"
    ON public.external_listings FOR SELECT
    USING (availability_status IN ('FRESH', 'STALE'));

-- 6. Matches
CREATE POLICY "Users can view matches for their own intents"
    ON public.matches FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.intents
            WHERE public.intents.id = public.matches.demand_intent_id
            AND public.intents.user_id = auth.uid()
        )
    );

-- 7. Notifications
CREATE POLICY "Users can view and update their own notifications"
    ON public.notifications FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 8. Reports (UGC Policy)
CREATE POLICY "Users can submit reports"
    ON public.reports FOR INSERT
    WITH CHECK (TRUE);

-- 9. Analytics
CREATE POLICY "Allow public event logging"
    ON public.analytics_events FOR INSERT
    WITH CHECK (TRUE);
