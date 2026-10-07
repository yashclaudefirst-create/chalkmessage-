-- Migration 001: Boards Table and RLS Policies

CREATE TABLE public.boards (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    code text NOT NULL,
    created_by uuid NOT NULL REFERENCES auth.users(id),
    created_by_name text NOT NULL,
    member_ids uuid[] NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    code_expires_at timestamptz NOT NULL
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.boards ENABLE ROW LEVEL SECURITY;

-- SELECT policy: User can read if their uid is in member_ids
CREATE POLICY "Users can view boards they are a member of"
ON public.boards
FOR SELECT
USING (auth.uid() = ANY(member_ids));

-- INSERT policy: User can insert if created_by is auth.uid() and member_ids equals [auth.uid()]
CREATE POLICY "Users can create boards as sole member"
ON public.boards
FOR INSERT
WITH CHECK (
    auth.uid() = created_by
    AND member_ids = ARRAY[auth.uid()]
    AND array_length(member_ids, 1) = 1
);

-- UPDATE policy: Member can update board, keeping member count <= 2 and staying as a member
CREATE POLICY "Members can update board"
ON public.boards
FOR UPDATE
USING (auth.uid() = ANY(member_ids))
WITH CHECK (
    array_length(member_ids, 1) <= 2
    AND auth.uid() = ANY(member_ids)
);

-- DELETE policy: No policy defined (deletions disabled by default)
